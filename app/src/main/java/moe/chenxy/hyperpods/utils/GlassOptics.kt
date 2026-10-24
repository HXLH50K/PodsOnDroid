package moe.chenxy.hyperpods.utils

internal object GlassOptics {
    const val EDGE_DP = 14f
    const val REFRACTION_DP = 7f
    const val BLUR_DP = 2f
    const val PADDING_DP = 6f

    fun selectionCenter(progress: Float, width: Float, density: Float, rtl: Boolean): Float {
        require(progress.isFinite() && width > 0 && density > 0)
        val padding = PADDING_DP * density
        val tabWidth = (width - padding * 2) / 2
        val fraction = progress.coerceIn(0f, 1f).let { if (rtl) 1f - it else it }
        return padding + tabWidth * (.5f + fraction)
    }

    val shader = """
        uniform shader backdrop;
        uniform float2 resolution;
        uniform float density;
        uniform float dark;
        uniform float selection;
        uniform float press;
        uniform float lightPosition;

        float3 capsule(float2 p, float2 center, float2 halfSize) {
            float radius = halfSize.y;
            float2 q = p - center;
            float axis = clamp(q.x, -max(halfSize.x - radius, 0.0), max(halfSize.x - radius, 0.0));
            float2 radial = q - float2(axis, 0.0);
            float length = max(length(radial), 0.001);
            return float3(radial / length, length - radius);
        }

        half4 main(float2 p) {
            float2 center = resolution * 0.5;
            float3 outer = capsule(p, center, center);
            float edge = pow(clamp(1.0 + outer.z / (${EDGE_DP} * density), 0.0, 1.0), 2.0);
            float2 padding = float2(${PADDING_DP} * density);
            float2 lensHalf = float2((resolution.x - padding.x * 2.0) * 0.25, center.y - padding.y);
            lensHalf.x += press * density;
            float2 lensCenter = float2(selection, center.y);
            float3 lens = capsule(p, lensCenter, lensHalf);
            float inside = 1.0 - smoothstep(-density, density, lens.z);
            float lensEdge = inside * pow(clamp(1.0 + lens.z / (9.0 * density), 0.0, 1.0), 2.0);
            float2 uv = p - outer.xy * edge * (${REFRACTION_DP} * density);
            uv -= lens.xy * lensEdge * (3.0 + press) * density;
            uv += (lensCenter - p) * inside * 0.025;
            uv = clamp(uv, float2(0.5), resolution - 0.5);
            half4 sample = backdrop.eval(uv);
            float chroma = edge * density * 0.35;
            float red = backdrop.eval(clamp(uv + outer.xy * chroma, float2(0.5), resolution - 0.5)).r;
            float blue = backdrop.eval(clamp(uv - outer.xy * chroma, float2(0.5), resolution - 0.5)).b;
            float3 scene = float3(red, sample.g, blue);
            float luma = dot(scene, float3(0.2126, 0.7152, 0.0722));
            scene = mix(float3(luma), scene, 1.1);
            float3 tint = mix(float3(0.98, 0.985, 1.0), float3(0.075, 0.10, 0.15), dark);
            float veil = mix(0.68, 0.72, dark) * (1.0 - edge * 0.65);
            veil += mix((1.0 - luma) * 0.06, luma * 0.10, dark) * (1.0 - edge * 0.65);
            float3 color = mix(scene, tint, veil);
            float lensVeil = inside * (1.0 - lensEdge * 0.65) * 0.88;
            color = mix(color, mix(float3(1.0), float3(0.10, 0.14, 0.21), dark), lensVeil);
            float2 light = normalize(float2((lightPosition / resolution.x - 0.5) * 0.7 - 0.3, -1.0));
            float rim = exp(-abs(outer.z + density * 0.65) / max(density * 0.7, 0.1));
            float specular = pow(max(dot(outer.xy, light), 0.0), 3.0) * rim;
            float innerRim = exp(-abs(lens.z + density * 0.5) / max(density * 0.65, 0.1)) * inside;
            color += float3(specular * mix(0.36, 0.22, dark));
            color += float3(innerRim * max(dot(lens.xy, light), 0.0) * (0.12 + press * 0.06));
            color -= float3(rim * max(dot(outer.xy, -light), 0.0) * 0.08);
            return half4(half3(clamp(color, 0.0, 1.0)), 1.0);
        }
    """.trimIndent()
}
