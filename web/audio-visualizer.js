/**
 * Karllo AI - Holographic JARVIS Audio Visualizer
 * =================================================
 * Interactive Canvas-based Arc-Reactor HUD
 * Supports real-time audio reactivity for:
 * - IDLE (Ambient breathing glow)
 * - LISTENING (Live microphone soundwaves & frequency rings)
 * - PROCESSING (Quantum data rotation & neural thinking sweep)
 * - SPEAKING (Harmonic vocal wave oscillations)
 */

export class JarvisVisualizer {
  constructor(canvas, voiceModule) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.voiceModule = voiceModule;

    this.rotation = 0;
    this.innerRotation = 0;
    this.pulsePhase = 0;
    this.isRunning = false;

    this.particles = [];
    this.initParticles(36);

    this.resize();
    window.addEventListener('resize', () => this.resize());
  }

  resize() {
    const rect = this.canvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    this.canvas.width = rect.width * dpr;
    this.canvas.height = rect.height * dpr;
    this.ctx.scale(dpr, dpr);
    this.width = rect.width;
    this.height = rect.height;
  }

  initParticles(count) {
    this.particles = [];
    for (let i = 0; i < count; i++) {
      const angle = (Math.PI * 2 * i) / count;
      this.particles.push({
        baseAngle: angle,
        distance: 70 + Math.random() * 40,
        speed: 0.005 + Math.random() * 0.01,
        size: 1.5 + Math.random() * 2,
        alpha: 0.3 + Math.random() * 0.7
      });
    }
  }

  start() {
    if (this.isRunning) return;
    this.isRunning = true;
    this.animate();
  }

  stop() {
    this.isRunning = false;
  }

  animate() {
    if (!this.isRunning) return;
    this.draw();
    requestAnimationFrame(() => this.animate());
  }

  draw() {
    const { ctx, width, height } = this;
    ctx.clearRect(0, 0, width, height);

    const centerX = width / 2;
    const centerY = height / 2;
    const state = this.voiceModule ? this.voiceModule.state : 'IDLE';

    // State parameters tuned for clean professional white background
    let baseColor = '#2563eb';      // Sapphire Blue default
    let glowColor = 'rgba(37, 99, 235, 0.12)';
    let speedMult = 1.0;
    let pulseIntensity = 0.05;

    if (state === 'LISTENING') {
      baseColor = '#059669';       // Emerald Green for listening
      glowColor = 'rgba(5, 150, 105, 0.18)';
      speedMult = 1.6;
      pulseIntensity = 0.15;
    } else if (state === 'PROCESSING') {
      baseColor = '#7c3aed';       // Violet for deep thinking
      glowColor = 'rgba(124, 58, 237, 0.18)';
      speedMult = 3.0;
      pulseIntensity = 0.2;
    } else if (state === 'SPEAKING') {
      baseColor = '#1d4ed8';       // Electric Royal Blue for speaking
      glowColor = 'rgba(29, 78, 216, 0.22)';
      speedMult = 1.8;
      pulseIntensity = 0.25;
    }

    this.rotation += 0.008 * speedMult;
    this.innerRotation -= 0.012 * speedMult;
    this.pulsePhase += 0.04 * speedMult;

    const pulse = 1 + Math.sin(this.pulsePhase) * pulseIntensity;

    // 1. Center Core Radial Glow
    const glowRadius = Math.min(width, height) * 0.38 * pulse;
    const gradient = ctx.createRadialGradient(centerX, centerY, 5, centerX, centerY, glowRadius);
    gradient.addColorStop(0, glowColor);
    gradient.addColorStop(0.5, 'rgba(241, 245, 249, 0.4)');
    gradient.addColorStop(1, 'rgba(255, 255, 255, 0)');
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(centerX, centerY, glowRadius, 0, Math.PI * 2);
    ctx.fill();

    // 2. Concentric Holographic HUD Rings
    const baseRadius = Math.min(width, height) * 0.28;

    // Outer Ring with Ticks
    this.drawHoloRing(centerX, centerY, baseRadius * 1.25, this.rotation, baseColor, 48, 4);

    // Mid Segmented Ring
    this.drawSegmentedRing(centerX, centerY, baseRadius * pulse, this.innerRotation, baseColor);

    // Inner Core Waveform or Reactor
    if (state === 'LISTENING') {
      this.drawAudioFrequencyWave(centerX, centerY, baseRadius * 0.75, baseColor);
    } else if (state === 'SPEAKING') {
      this.drawSpeechHarmonics(centerX, centerY, baseRadius * 0.75, baseColor);
    } else if (state === 'PROCESSING') {
      this.drawQuantumScanningSweep(centerX, centerY, baseRadius * 0.7, baseColor);
    } else {
      this.drawIdlePulseRing(centerX, centerY, baseRadius * 0.6, pulse, baseColor);
    }

    // 3. Floating Holographic Particles
    this.drawParticles(centerX, centerY, baseColor);
  }

  drawHoloRing(cx, cy, r, rot, color, ticks, tickLength) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(rot);

    ctx.strokeStyle = color;
    ctx.lineWidth = 1;
    ctx.globalAlpha = 0.5;

    ctx.beginPath();
    ctx.arc(0, 0, r, 0, Math.PI * 2);
    ctx.stroke();

    for (let i = 0; i < ticks; i++) {
      const angle = (Math.PI * 2 * i) / ticks;
      const x1 = Math.cos(angle) * (r - tickLength);
      const y1 = Math.sin(angle) * (r - tickLength);
      const x2 = Math.cos(angle) * (r + tickLength);
      const y2 = Math.sin(angle) * (r + tickLength);

      ctx.beginPath();
      ctx.moveTo(x1, y1);
      ctx.lineTo(x2, y2);
      ctx.stroke();
    }
    ctx.restore();
  }

  drawSegmentedRing(cx, cy, r, rot, color) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(rot);
    ctx.strokeStyle = color;
    ctx.lineWidth = 2.5;
    ctx.globalAlpha = 0.8;

    const segments = 4;
    const gap = 0.3;
    const segAngle = (Math.PI * 2) / segments;

    for (let i = 0; i < segments; i++) {
      const start = i * segAngle + gap;
      const end = (i + 1) * segAngle - gap;
      ctx.beginPath();
      ctx.arc(0, 0, r, start, end);
      ctx.stroke();
    }
    ctx.restore();
  }

  drawAudioFrequencyWave(cx, cy, r, color) {
    const { ctx } = this;
    const freqData = this.voiceModule ? this.voiceModule.getAudioFrequencyData() : null;

    ctx.save();
    ctx.translate(cx, cy);
    ctx.strokeStyle = color;
    ctx.lineWidth = 2;
    ctx.globalAlpha = 0.9;
    ctx.beginPath();

    const points = 64;
    for (let i = 0; i <= points; i++) {
      const angle = (Math.PI * 2 * i) / points;
      let amp = 0;
      if (freqData && freqData.length > 0) {
        const idx = Math.floor((i / points) * (freqData.length / 2));
        amp = (freqData[idx] / 255) * 35;
      } else {
        amp = Math.sin(angle * 6 + this.pulsePhase * 3) * 12;
      }
      const curR = r + amp;
      const x = Math.cos(angle) * curR;
      const y = Math.sin(angle) * curR;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.closePath();
    ctx.stroke();
    ctx.restore();
  }

  drawSpeechHarmonics(cx, cy, r, color) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.strokeStyle = color;
    ctx.lineWidth = 2.5;
    ctx.globalAlpha = 0.95;

    for (let wave = 0; wave < 3; wave++) {
      ctx.beginPath();
      const points = 72;
      const waveR = r - wave * 12;
      for (let i = 0; i <= points; i++) {
        const angle = (Math.PI * 2 * i) / points;
        const offset = Math.sin(angle * (4 + wave * 2) + this.pulsePhase * 4) * (8 + wave * 4);
        const x = Math.cos(angle) * (waveR + offset);
        const y = Math.sin(angle) * (waveR + offset);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.closePath();
      ctx.stroke();
    }
    ctx.restore();
  }

  drawQuantumScanningSweep(cx, cy, r, color) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(this.rotation * 4);

    const gradient = ctx.createConicGradient(0, 0, 0);
    gradient.addColorStop(0, 'rgba(188, 19, 254, 0)');
    gradient.addColorStop(0.7, 'rgba(188, 19, 254, 0.1)');
    gradient.addColorStop(1, 'rgba(188, 19, 254, 0.8)');

    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(0, 0, r, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = color;
    ctx.lineWidth = 1.5;
    ctx.stroke();
    ctx.restore();
  }

  drawIdlePulseRing(cx, cy, r, pulse, color) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.strokeStyle = color;
    ctx.lineWidth = 1.8;
    ctx.globalAlpha = 0.6;

    ctx.beginPath();
    ctx.arc(0, 0, r * pulse, 0, Math.PI * 2);
    ctx.stroke();

    // Central core dot
    ctx.fillStyle = color;
    ctx.globalAlpha = 0.9;
    ctx.beginPath();
    ctx.arc(0, 0, 7 * pulse, 0, Math.PI * 2);
    ctx.fill();

    ctx.restore();
  }

  drawParticles(cx, cy, color) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.fillStyle = color;

    for (const p of this.particles) {
      p.baseAngle += p.speed;
      const x = Math.cos(p.baseAngle) * p.distance;
      const y = Math.sin(p.baseAngle) * p.distance;

      ctx.globalAlpha = p.alpha;
      ctx.beginPath();
      ctx.arc(x, y, p.size, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.restore();
  }
}
