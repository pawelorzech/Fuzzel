package com.fizzy.android.data.repository

import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.core.network.InstanceManager
import com.fizzy.android.data.api.FizzyApiService
import com.fizzy.android.data.api.dto.RequestMagicLinkRequest
import com.fizzy.android.data.api.dto.VerifyMagicLinkRequest
import com.fizzy.android.data.api.dto.getAccountId
import com.fizzy.android.data.api.dto.getAccountSlug
import com.fizzy.android.data.api.dto.toDomain
import com.fizzy.android.data.api.dto.toUser
import com.fizzy.android.data.local.AccountStorage
import com.fizzy.android.domain.model.Account
import com.fizzy.android.domain.model.User
import com.fizzy.android.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: FizzyApiService,
    private val accountStorage: AccountStorage,
    private val instanceManager: InstanceManager
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = accountStorage.activeAccount.map { it != null }

    override val currentAccount: Flow<Account?> = accountStorage.activeAccount

    override val allAccounts: Flow<List<Account>> = accountStorage.accounts

    // Store pending authentication token for magic link verification
    private var pendingAuthToken: String? = null
    private var pendingEmail: String? = null

    override suspend fun requestMagicLink(instanceUrl: String, email: String): ApiResult<Unit> {
        // Temporarily set the instance for this request
        instanceManager.setInstance(instanceUrl, "")

        return ApiResult.from {
            apiService.requestMagicLink(RequestMagicLinkRequest(emailAddress = email))
        }.map { response ->
            // Store the pending auth token and email for verification step
            pendingAuthToken = response.pendingAuthenticationToken
            pendingEmail = email
        }
    }

    override suspend fun verifyMagicLink(instanceUrl: String, email: String, code: String): ApiResult<Account> {
        instanceManager.setInstance(instanceUrl, "")

        return ApiResult.from {
            apiService.verifyMagicLink(VerifyMagicLinkRequest(code = code))
        }.mapSuspend { response ->
            val token = response.sessionToken

            // Set the token to fetch user identity
            instanceManager.setInstance(instanceUrl, token)

            // Fetch user identity with the new token
            val identityResult = ApiResult.from { apiService.getCurrentIdentity() }
            val identity = when (identityResult) {
                is ApiResult.Success -> identityResult.data
                is ApiResult.Error -> throw Exception("Failed to fetch identity: ${identityResult.message}")
                is ApiResult.Exception -> throw identityResult.throwable
            }

            val user = identity.toUser()
            val accountId = identity.getAccountId()
            val accountSlug = identity.getAccountSlug()

            val account = Account(
                id = UUID.randomUUID().toString(),
                instanceUrl = instanceManager.getBaseUrl() ?: instanceUrl,
                email = pendingEmail ?: email,
                token = token,
                userName = user.name,
                userId = user.id,
                avatarUrl = user.avatarUrl,
                isActive = true,
                fizzyAccountId = accountId,
                fizzyAccountSlug = accountSlug
            )

            instanceManager.setInstance(account.instanceUrl, account.token, accountSlug)
            accountStorage.addAccount(account)

            // Clear pending state
            pendingAuthToken = null
            pendingEmail = null

            account
        }
    }

    override suspend fun loginWithToken(instanceUrl: String, token: String): ApiResult<Account> {
        instanceManager.setInstance(instanceUrl, token)

        val result = ApiResult.from {
            apiService.getCurrentIdentity()
        }

        return when (result) {
            is ApiResult.Success -> {
                val identity = result.data
                val domainUser = identity.toUser()
                val accountId = identity.getAccountId()
                val accountSlug = identity.getAccountSlug()

                android.util.Log.d("AuthRepository", "Identity loaded: ${domainUser.email}, accountId: $accountId, slug: $accountSlug")

                val account = Account(
                    id = UUID.randomUUID().toString(),
                    instanceUrl = instanceManager.getBaseUrl() ?: instanceUrl,
                    email = domainUser.email,
                    token = token,
                    userName = domainUser.name,
                    userId = domainUser.id,
                    avatarUrl = domainUser.avatarUrl,
                    isActive = true,
                    fizzyAccountId = accountId,
                    fizzyAccountSlug = accountSlug
                )

                // Update instance manager with account slug for API calls
                instanceManager.setInstance(account.instanceUrl, account.token, accountSlug)
                accountStorage.addAccount(account)
                ApiResult.Success(account)
            }
            is ApiResult.Error -> {
                android.util.Log.e("AuthRepository", "Token login failed: ${result.code} - ${result.message}")
                ApiResult.Error(result.code, "Auth failed (${result.code}): ${result.message}")
            }
            is ApiResult.Exception -> {
                android.util.Log.e("AuthRepository", "Token login exception", result.throwable)
                ApiResult.Exception(result.throwable)
            }
        }
    }

    override suspend fun getCurrentUser(): ApiResult<User> {
        return ApiResult.from {
            apiService.getCurrentUser()
        }.map { it.toDomain() }
    }

    override suspend fun switchAccount(accountId: String) {
        val account = accountStorage.getAccount(accountId) ?: return
        accountStorage.setActiveAccount(accountId)
        instanceManager.setInstance(account.instanceUrl, account.token, account.fizzyAccountSlug)
    }

    override suspend fun logout(accountId: String) {
        accountStorage.removeAccount(accountId)

        // If there's another account, switch to it
        val remainingAccount = accountStorage.getActiveAccount()
        if (remainingAccount != null) {
            instanceManager.setInstance(remainingAccount.instanceUrl, remainingAccount.token)
        } else {
            instanceManager.clearInstance()
        }
    }

    override suspend fun logoutAll() {
        accountStorage.clearAll()
        instanceManager.clearInstance()
    }

    override suspend fun initializeActiveAccount() {
        val activeAccount = accountStorage.getActiveAccount()
        if (activeAccount != null) {
            instanceManager.setInstance(activeAccount.instanceUrl, activeAccount.token, activeAccount.fizzyAccountSlug)
        }
    }
}
