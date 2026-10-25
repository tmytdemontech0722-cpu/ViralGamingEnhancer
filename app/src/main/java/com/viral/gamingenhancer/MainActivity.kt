package com.viral.gamingenhancer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.viral.gamingenhancer.ui.AppRoot
import com.viral.gamingenhancer.ui.EditorViewModel
import com.viral.gamingenhancer.ui.VgeTheme

class MainActivity : ComponentActivity() {
    private val vm: EditorViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VgeTheme { AppRoot(vm) } }
    }
}
