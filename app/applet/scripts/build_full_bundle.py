import os

with open('web/karllo-core.js', 'r', encoding='utf-8') as f:
    core_code = f.read().replace('export class KarlloCoreEngine', 'class KarlloCoreEngine')

with open('web/speech-module.js', 'r', encoding='utf-8') as f:
    voice_code = f.read().replace('export class KarlloVoiceModule', 'class KarlloVoiceModule')

with open('web/audio-visualizer.js', 'r', encoding='utf-8') as f:
    vis_code = f.read().replace('export class JarvisVisualizer', 'class JarvisVisualizer')

with open('web/app.js', 'r', encoding='utf-8') as f:
    app_code = f.read()

# Filter imports
app_lines = [l for l in app_code.splitlines() if not l.startswith('import ')]
app_code_clean = '\n'.join(app_lines)

full_bundle = f"""/**
 * Karllo AI - Consolidated Unified Script Bundle
 * Zero-dependency, offline-ready, high-performance runtime
 */

window.addEventListener('error', function(e) {{
  console.error('[Karllo Runtime Error]', e.message, e.filename, e.lineno);
}});

{core_code}

{voice_code}

{vis_code}

{app_code_clean}
"""

with open('web/bundle.js', 'w', encoding='utf-8') as f:
    f.write(full_bundle)

print('bundle.js generated successfully. Size:', len(full_bundle))
