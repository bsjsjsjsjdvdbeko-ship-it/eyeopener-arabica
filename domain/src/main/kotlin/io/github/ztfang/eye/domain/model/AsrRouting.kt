package io.github.ztfang.eye.domain.model

/**
 * ASR 路由单一事实源（Single Source of Truth）。
 *
 * 路由规则（优先级从高到低）：
 *  1. zh / en        → SHERPA_ONNX          (X-ASR 中英模型)
 *  2. bn             → SHERPA_ONNX_BN       (孟加拉语专用模型)
 *  3. auto           → SHERPA_ONNX_NEMOTRON (كشف تلقائي عن اللغة)
 *  4. Nemotron 26 语 → SHERPA_ONNX_NEMOTRON (多语种大包)
 *  5. 其余语言        → VOSK                (按语言单独小包兜底)
 */
data class AsrRoute(
    val languageCode: String,
    val engine: AsrEngineType,
    /** Sherpa-ONNX 模型 ID；Vosk 兜底为 null */
    val modelId: String?,
)

object AsrRoutingTable {
    /** X-ASR 覆盖语言（zh/en 共享同一模型） */
    private val X_ASR_LANGS = setOf("zh", "en")

    /** BN 专用语言 */
    private const val BN_LANG = "bn"

    /** الكشف التلقائي عن اللغة */
    private const val AUTO_LANG = "auto"

    /** 显式路由表（除 Vosk 兜底外的所有语言） */
    private val explicitRoutes: Map<String, AsrRoute> by lazy {
        buildMap {
            for (lang in X_ASR_LANGS) {
                put(
                    lang,
                    AsrRoute(
                        lang,
                        AsrEngineType.SHERPA_ONNX,
                        SherpaOnnxModel.X_ASR_ZH_EN_960MS.modelId,
                    ),
                )
            }
            put(
                AUTO_LANG,
                AsrRoute(
                    AUTO_LANG,
                    AsrEngineType.SHERPA_ONNX_NEMOTRON,
                    SherpaOnnxModel.NEMOTRON_3_5_320MS_INT8.modelId,
                ),
            )
            put(
                BN_LANG,
                AsrRoute(
                    BN_LANG,
                    AsrEngineType.SHERPA_ONNX_BN,
                    SherpaOnnxModel.BN_VOSK_2026_02_09.modelId,
                ),
            )
            for (lang in SherpaOnnxModel.NEMOTRON_LANGUAGES
                .filter { it !in X_ASR_LANGS && it != BN_LANG }) {
                put(
                    lang,
                    AsrRoute(
                        lang,
                        AsrEngineType.SHERPA_ONNX_NEMOTRON,
                        SherpaOnnxModel.NEMOTRON_3_5_320MS_INT8.modelId,
                    ),
                )
            }
        }
    }

    /** 某语言的完整路由；未显式声明的语言一律回退 Vosk */
    fun forLanguage(code: String): AsrRoute = explicitRoutes[code] ?: AsrRoute(code, AsrEngineType.VOSK, null)

    /** 语言 → 引擎 */
    fun engineFor(code: String): AsrEngineType = forLanguage(code).engine

    /** 语言 → Sherpa 模型 ID（Vosk 兜底返回 null） */
    fun modelIdFor(code: String): String? = forLanguage(code).modelId

    /** 被 Sherpa-ONNX 覆盖的语言集合（Vosk 下载列表不再显示这些） */
    val sherpaCoveredLanguages: Set<String> get() = explicitRoutes.keys
}
