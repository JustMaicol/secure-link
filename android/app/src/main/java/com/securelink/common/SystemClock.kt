package com.securelink.common

import com.securelink.core.common.Clock

// spec 100 · Única clase del proyecto que llama a System.currentTimeMillis() (regla 6).
class SystemClock : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
