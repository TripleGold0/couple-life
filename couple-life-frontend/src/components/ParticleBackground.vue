<template>
  <div class="particle-background" aria-hidden="true">
    <span
      v-for="particle in particles"
      :key="particle.id"
      class="particle"
      :style="{
        '--x': particle.x + '%',
        '--y': particle.y + '%',
        '--size': particle.size + 'px',
        '--delay': particle.delay + 's',
        '--duration': particle.duration + 's',
        '--drift': particle.drift + 'px',
        '--alpha': particle.alpha,
        '--hue': particle.hue
      }"
    >
      <span class="particle-halo" />
      <span class="particle-core" />
    </span>
  </div>
</template>

<script setup>
const particles = [
  { id: 1, x: 7, y: 12, size: 12, delay: -1, duration: 12, drift: -34, alpha: 0.78, hue: '255, 111, 159' },
  { id: 2, x: 16, y: 66, size: 9, delay: -8, duration: 16, drift: 42, alpha: 0.62, hue: '181, 140, 255' },
  { id: 3, x: 25, y: 28, size: 7, delay: -4, duration: 10, drift: 28, alpha: 0.76, hue: '255, 255, 255' },
  { id: 4, x: 38, y: 76, size: 11, delay: -10, duration: 17, drift: -38, alpha: 0.58, hue: '255, 201, 169' },
  { id: 5, x: 51, y: 18, size: 14, delay: -6, duration: 15, drift: 40, alpha: 0.64, hue: '255, 111, 159' },
  { id: 6, x: 64, y: 56, size: 8, delay: -12, duration: 13, drift: -30, alpha: 0.72, hue: '255, 255, 255' },
  { id: 7, x: 76, y: 34, size: 10, delay: -3, duration: 18, drift: 46, alpha: 0.56, hue: '181, 140, 255' },
  { id: 8, x: 88, y: 82, size: 13, delay: -9, duration: 14, drift: -42, alpha: 0.64, hue: '255, 111, 159' },
  { id: 9, x: 11, y: 86, size: 8, delay: -14, duration: 17, drift: 34, alpha: 0.52, hue: '255, 201, 169' },
  { id: 10, x: 22, y: 46, size: 15, delay: -2, duration: 20, drift: -48, alpha: 0.5, hue: '255, 255, 255' },
  { id: 11, x: 34, y: 8, size: 9, delay: -11, duration: 12, drift: 26, alpha: 0.7, hue: '255, 111, 159' },
  { id: 12, x: 47, y: 92, size: 7, delay: -7, duration: 11, drift: -26, alpha: 0.58, hue: '181, 140, 255' },
  { id: 13, x: 59, y: 40, size: 16, delay: -15, duration: 21, drift: 42, alpha: 0.48, hue: '255, 214, 231' },
  { id: 14, x: 72, y: 12, size: 10, delay: -5, duration: 15, drift: -32, alpha: 0.66, hue: '255, 255, 255' },
  { id: 15, x: 84, y: 52, size: 12, delay: -13, duration: 18, drift: 38, alpha: 0.52, hue: '255, 111, 159' },
  { id: 16, x: 94, y: 24, size: 7, delay: -6, duration: 13, drift: -24, alpha: 0.78, hue: '181, 140, 255' },
  { id: 17, x: 5, y: 38, size: 15, delay: -16, duration: 22, drift: 40, alpha: 0.46, hue: '255, 255, 255' },
  { id: 18, x: 31, y: 70, size: 9, delay: -4, duration: 14, drift: -34, alpha: 0.58, hue: '255, 111, 159' },
  { id: 19, x: 57, y: 66, size: 11, delay: -9, duration: 16, drift: 30, alpha: 0.54, hue: '181, 140, 255' },
  { id: 20, x: 92, y: 70, size: 14, delay: -12, duration: 19, drift: -46, alpha: 0.5, hue: '255, 201, 169' },
  { id: 21, x: 14, y: 20, size: 7, delay: -18, duration: 12, drift: 32, alpha: 0.68, hue: '255, 255, 255' },
  { id: 22, x: 44, y: 50, size: 10, delay: -20, duration: 15, drift: -36, alpha: 0.56, hue: '255, 111, 159' },
  { id: 23, x: 70, y: 88, size: 12, delay: -17, duration: 17, drift: 44, alpha: 0.48, hue: '181, 140, 255' },
  { id: 24, x: 82, y: 6, size: 8, delay: -21, duration: 14, drift: -28, alpha: 0.7, hue: '255, 255, 255' },
  { id: 25, x: 40, y: 22, size: 6, delay: -5, duration: 9, drift: 20, alpha: 0.9, hue: '255, 255, 255' },
  { id: 26, x: 68, y: 72, size: 6, delay: -7, duration: 10, drift: -22, alpha: 0.84, hue: '255, 255, 255' },
  { id: 27, x: 97, y: 42, size: 9, delay: -12, duration: 16, drift: -36, alpha: 0.62, hue: '255, 111, 159' },
  { id: 28, x: 2, y: 72, size: 10, delay: -3, duration: 17, drift: 38, alpha: 0.58, hue: '181, 140, 255' }
]
</script>

<style scoped>
.particle-background {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  left: var(--x);
  top: var(--y);
  width: var(--size);
  height: var(--size);
  animation: particle-drift var(--duration) ease-in-out infinite;
  animation-delay: var(--delay);
  transform: translate3d(0, 0, 0);
}

.particle::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 1px;
  height: calc(var(--size) * 3.4);
  border-radius: 999px;
  background: linear-gradient(to bottom, rgba(var(--hue), 0.72), rgba(var(--hue), 0));
  opacity: 0.42;
  transform: translate(-50%, -16%) rotate(28deg);
  filter: blur(0.4px);
}

.particle-halo,
.particle-core {
  position: absolute;
  inset: 0;
  border-radius: 999px;
}

.particle-halo {
  background: radial-gradient(circle, rgba(var(--hue), calc(var(--alpha) * 0.92)) 0%, rgba(var(--hue), 0.34) 32%, rgba(var(--hue), 0) 72%);
  filter: blur(6px);
  transform: scale(3.1);
}

.particle-core {
  background: rgba(var(--hue), var(--alpha));
  box-shadow:
    0 0 12px rgba(var(--hue), 0.82),
    0 0 28px rgba(236, 72, 153, 0.34),
    0 0 46px rgba(181, 140, 255, 0.24);
}

.particle:nth-child(5n) .particle-core {
  border-radius: 40% 60% 45% 55%;
  transform: rotate(18deg);
}

@keyframes particle-drift {
  0%, 100% {
    opacity: 0.46;
    transform: translate3d(0, 0, 0) scale(0.92);
  }
  50% {
    opacity: 1;
    transform: translate3d(var(--drift), -46px, 0) scale(1.24);
  }
}

@media (max-width: 640px) {
  .particle:nth-child(n + 21) {
    display: none;
  }

  .particle {
    opacity: 0.9;
  }

  .particle-halo {
    transform: scale(2.55);
  }
}

@media (prefers-reduced-motion: reduce) {
  .particle {
    animation: none;
  }
}
</style>
