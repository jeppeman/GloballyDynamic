package com.jeppeman.locallydynamic;

import androidx.annotation.Nullable;

class Objects {
    static boolean equals(@Nullable Object obj1, @Nullable Object obj2){
        return (obj1 == null && obj2 == null) || (obj1 != null && obj1.equals(obj2));
    }
}
