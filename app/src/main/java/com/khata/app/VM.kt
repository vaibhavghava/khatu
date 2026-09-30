package com.khata.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class Bal(val customer: Customer, val given: Double, val received: Double) {
    /** BALANCE = TOTAL GIVEN - TOTAL RECEIVED. Positive = receivable, negative = payable. */
    val net: Double get() = r2(given - received)
}

class KhataVM(app: Application) : AndroidViewModel(app) {
    private val db = KhataDb.get(app)

    val txns: StateFlow<List<Txn>> = db.txns().all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val balances: StateFlow<List<Bal>> = combine(db.customers().all(), txns) { cs, ts ->
        val g = HashMap<Long, Double>()
        val r = HashMap<Long, Double>()
        ts.forEach { val m = if (it.type == T.G) g else r; m[it.customerId] = (m[it.customerId] ?: 0.0) + it.amount }
        cs.map { Bal(it, r2(g[it.id] ?: 0.0), r2(r[it.id] ?: 0.0)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val profile: StateFlow<Profile?> = db.profile().get().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val theme: StateFlow<String?> = db.settings().get().map { it?.theme ?: "SYSTEM" }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun saveCustomer(c: Customer, done: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = if (c.id == 0L) db.customers().insert(c) else { db.customers().update(c); c.id }
        done(id)
    }
    fun deleteCustomer(c: Customer) = viewModelScope.launch { db.customers().delete(c) }
    fun saveTxn(t: Txn) = viewModelScope.launch { if (t.id == 0L) db.txns().insert(t) else db.txns().update(t) }
    fun deleteTxn(t: Txn) = viewModelScope.launch { db.txns().delete(t) }
    fun saveProfile(p: Profile) = viewModelScope.launch { db.profile().save(p) }
    fun setTheme(t: String) = viewModelScope.launch { db.settings().save(Settings(1, t)) }
}
