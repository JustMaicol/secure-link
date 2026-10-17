package com.senda.common

import com.senda.core.common.Clock

// spec 100 · Única clase del proyecto que llama a System.currentTimeMillis() (regla 6).
class SystemClock : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
