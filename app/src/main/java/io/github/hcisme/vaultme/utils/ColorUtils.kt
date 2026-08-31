package io.github.hcisme.vaultme.utils

import androidx.compose.ui.graphics.Color
import java.util.Locale

/**
 * 根据平台名称获取对应的品牌颜色或计算随机颜色
 */
object ColorUtils {

    /**
     * 根据平台名称获取颜色
     * @param platform 平台名称 (如 "Google", "GitHub")
     * @return 对应的 Compose Color 对象
     */
    fun getPlatformColor(platform: String): Color {
        return when (platform.trim().lowercase(Locale.getDefault())) {
            "google" -> Color(0xFF4285F4)
            "github" -> Color(0xFF24292E)
            "amazon" -> Color(0xFFFF9900)
            "netflix" -> Color(0xFFE50914)
            "twitter", "x" -> Color(0xFF000000)
            "facebook" -> Color(0xFF1877F2)
            "microsoft" -> Color(0xFF00A4EF)
            "apple" -> Color(0xFF000000)
            "wechat", "微信" -> Color(0xFF07C160)
            "taobao", "淘宝" -> Color(0xFFFF5000)
            "weibo", "微博" -> Color(0xFFE6162D)
            else -> generateColorFromText(platform)
        }
    }

    /**
     * 根据文本哈希值生成一个稳定的颜色，确保相同文本总是得到相同颜色
     */
    private fun generateColorFromText(text: String): Color {
        if (text.isBlank()) return Color.Gray
        
        val hash = text.hashCode()
        // 使用哈希值生成 RGB 分量，确保颜色不会太浅或太深
        val r = (hash and 0xFF0000 shr 16) % 200 + 30
        val g = (hash and 0x00FF00 shr 8) % 200 + 30
        val b = (hash and 0x0000FF) % 200 + 30
        
        return Color(r, g, b)
    }
}
