package com.fizzy.android.domain.repository

import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.domain.model.Account
import com.fizzy.android.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    val currentAccount: Flow<Account?>
    val allAccounts: Flow<List<Account>>

    suspend fun requestMagicLink(instanceUrl: String, email: String): ApiResult<Unit>
    suspend fun verifyMagicLink(instanceUrl: String, email: String, code: String): ApiResult<Account>
    suspend fun loginWithToken(instanceUrl: String, token: String): ApiResult<Account>
    suspend fun getCurrentUser(): ApiResult<User>

    suspend fun switchAccount(accountId: String)
    suspend fun logout(accountId: String)
    suspend fun logoutAll()

    suspend fun initializeActiveAccount()
}
