package com.fizzy.android.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fizzy.android.domain.model.Account
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AccountStorage {
    val accounts: Flow<List<Account>>
    val activeAccount: Flow<Account?>

    suspend fun addAccount(account: Account)
    suspend fun removeAccount(accountId: String)
    suspend fun setActiveAccount(accountId: String)
    suspend fun updateAccount(account: Account)
    suspend fun getAccount(accountId: String): Account?
    suspend fun getActiveAccount(): Account?
    suspend fun getAllAccounts(): List<Account>
    suspend fun clearAll()
}

@Singleton
class AccountStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) : AccountStorage {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "fizzy_accounts",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val accountListType = Types.newParameterizedType(List::class.java, AccountData::class.java)
    private val accountAdapter = moshi.adapter<List<AccountData>>(accountListType)

    private val _accountsFlow = MutableStateFlow<List<Account>>(loadAccounts())
    override val accounts: Flow<List<Account>> = _accountsFlow.asStateFlow()

    override val activeAccount: Flow<Account?> = _accountsFlow.map { accounts ->
        accounts.find { it.isActive }
    }

    private fun loadAccounts(): List<Account> {
        val json = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            accountAdapter.fromJson(json)?.map { it.toAccount() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveAccounts(accounts: List<Account>) {
        val data = accounts.map { AccountData.fromAccount(it) }
        val json = accountAdapter.toJson(data)
        prefs.edit().putString(KEY_ACCOUNTS, json).apply()
        _accountsFlow.value = accounts
    }

    override suspend fun addAccount(account: Account) {
        val currentAccounts = _accountsFlow.value.toMutableList()

        // Deactivate all existing accounts
        val updatedAccounts = currentAccounts.map { it.copy(isActive = false) }.toMutableList()

        // Add new account as active
        updatedAccounts.add(account.copy(isActive = true))

        saveAccounts(updatedAccounts)
    }

    override suspend fun removeAccount(accountId: String) {
        val currentAccounts = _accountsFlow.value.toMutableList()
        val wasActive = currentAccounts.find { it.id == accountId }?.isActive == true

        currentAccounts.removeAll { it.id == accountId }

        // If removed account was active, activate the first remaining account
        if (wasActive && currentAccounts.isNotEmpty()) {
            currentAccounts[0] = currentAccounts[0].copy(isActive = true)
        }

        saveAccounts(currentAccounts)
    }

    override suspend fun setActiveAccount(accountId: String) {
        val currentAccounts = _accountsFlow.value.map { account ->
            account.copy(isActive = account.id == accountId)
        }
        saveAccounts(currentAccounts)
    }

    override suspend fun updateAccount(account: Account) {
        val currentAccounts = _accountsFlow.value.map { existing ->
            if (existing.id == account.id) account else existing
        }
        saveAccounts(currentAccounts)
    }

    override suspend fun getAccount(accountId: String): Account? {
        return _accountsFlow.value.find { it.id == accountId }
    }

    override suspend fun getActiveAccount(): Account? {
        return _accountsFlow.value.find { it.isActive }
    }

    override suspend fun getAllAccounts(): List<Account> {
        return _accountsFlow.value
    }

    override suspend fun clearAll() {
        prefs.edit().clear().apply()
        _accountsFlow.value = emptyList()
    }

    companion object {
        private const val KEY_ACCOUNTS = "accounts"
    }
}

// Internal data class for JSON serialization
private data class AccountData(
    val id: String,
    val instanceUrl: String,
    val email: String,
    val token: String,
    val userName: String,
    val userId: Long,
    val avatarUrl: String?,
    val isActive: Boolean,
    val fizzyAccountId: String? = null,
    val fizzyAccountSlug: String? = null
) {
    fun toAccount() = Account(
        id = id,
        instanceUrl = instanceUrl,
        email = email,
        token = token,
        userName = userName,
        userId = userId,
        avatarUrl = avatarUrl,
        isActive = isActive,
        fizzyAccountId = fizzyAccountId,
        fizzyAccountSlug = fizzyAccountSlug
    )

    companion object {
        fun fromAccount(account: Account) = AccountData(
            id = account.id,
            instanceUrl = account.instanceUrl,
            email = account.email,
            token = account.token,
            userName = account.userName,
            userId = account.userId,
            avatarUrl = account.avatarUrl,
            isActive = account.isActive,
            fizzyAccountId = account.fizzyAccountId,
            fizzyAccountSlug = account.fizzyAccountSlug
        )
    }
}
