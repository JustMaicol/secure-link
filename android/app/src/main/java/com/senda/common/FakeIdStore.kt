package com.senda.common

// spec 100 · IdStore en memoria para pruebas.
class FakeIdStore(var value: String? = null) : IdStore {
    var writes: Int = 0
        private set

    override fun read(): String? = value

    override fun write(value: String) {
        this.value = value
        writes++
    }
}
