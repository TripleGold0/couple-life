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
        '--rise': particle.rise + 'px',
        '--alpha': particle.alpha,
        '--trail': particle.trail + 'px',
        '--hue': particle.hue
      }"
    />
  </div>
</template>

<script setup>
const particles = [
  { id: 1, x: 5, y: 13, size: 9, delay: -2, duration: 9, drift: 18, rise: 68, alpha: 0.72, trail: 72, hue: '201 54 101' },
  { id: 2, x: 14, y: 76, size: 7, delay: -8, duration: 12, drift: 24, rise: 58, alpha: 0.62, trail: 58, hue: '116 90 194' },
  { id: 3, x: 82, y: 12, size: 10, delay: -5, duration: 10, drift: -22, rise: 72, alpha: 0.68, trail: 76, hue: '201 54 101' },
  { id: 4, x: 91, y: 69, size: 8, delay: -3, duration: 8, drift: -18, rise: 62, alpha: 0.70, trail: 62, hue: '116 90 194' },
  { id: 5, x: 7, y: 45, size: 6, delay: -7, duration: 11, drift: 20, rise: 52, alpha: 0.58, trail: 52, hue: '201 54 101' },
  { id: 6, x: 87, y: 42, size: 6, delay: -10, duration: 13, drift: -24, rise: 56, alpha: 0.64, trail: 54, hue: '201 54 101' },
  { id: 7, x: 24, y: 8, size: 5, delay: -4, duration: 10, drift: 16, rise: 48, alpha: 0.50, trail: 46, hue: '116 90 194' },
  { id: 8, x: 74, y: 86, size: 7, delay: -6, duration: 11, drift: -20, rise: 58, alpha: 0.60, trail: 58, hue: '201 54 101' },
  { id: 9, x: 31, y: 90, size: 5, delay: -9, duration: 12, drift: 18, rise: 46, alpha: 0.50, trail: 44, hue: '201 54 101' },
  { id: 10, x: 69, y: 6, size: 6, delay: -1, duration: 9, drift: -16, rise: 54, alpha: 0.54, trail: 50, hue: '116 90 194' },
  { id: 11, x: 3, y: 88, size: 5, delay: -11, duration: 13, drift: 22, rise: 48, alpha: 0.48, trail: 42, hue: '201 54 101' },
  { id: 12, x: 95, y: 28, size: 5, delay: -5, duration: 10, drift: -20, rise: 50, alpha: 0.56, trail: 46, hue: '116 90 194' }
]
</script>

<style scoped>
.particle-background {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.particle-background::before {
  content: '';
  position: absolute;
  inset: -15%;
  background:
    radial-gradient(circle at 12% 18%, rgb(201 54 101 / 8.5%), transparent 30%),
    radial-gradient(circle at 88% 76%, rgb(116 90 194 / 8.5%), transparent 28%);
}

.particle {
  position: absolute;
  left: var(--x);
  top: var(--y);
  width: var(--size);
  height: var(--size);
  border-radius: 50%;
  background: rgb(var(--hue) / var(--alpha));
  box-shadow:
    0 0 0 calc(var(--size) * 0.7) rgb(var(--hue) / 6%),
    0 0 calc(var(--size) * 3) rgb(var(--hue) / 26%);
  animation: particle-rise var(--duration) cubic-bezier(0.45, 0, 0.55, 1) infinite alternate;
  animation-delay: var(--delay);
  transform: translate3d(0, 0, 0);
}

.particle::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 4px;
  width: 1px;
  height: var(--trail);
  border-radius: 1px;
  background: linear-gradient(to top, rgb(var(--hue) / 28%), transparent);
  transform: translateX(-50%) rotate(-12deg);
  transform-origin: bottom;
}

@keyframes particle-rise {
  from {
    opacity: 0.55;
    transform: translate3d(calc(var(--drift) * -0.35), 24px, 0) scale(0.84);
  }
  to {
    opacity: 1;
    transform: translate3d(var(--drift), calc(var(--rise) * -1), 0) scale(1.12);
  }
}

@media (max-width: 640px) {
  .particle:nth-child(n + 9) {
    display: none;
  }

  .particle {
    box-shadow:
      0 0 0 calc(var(--size) * 0.55) rgb(var(--hue) / 5%),
      0 0 calc(var(--size) * 2.2) rgb(var(--hue) / 22%);
  }

  .particle::after {
    height: calc(var(--trail) * 0.72);
  }
}

@media (prefers-reduced-motion: reduce) {
  .particle {
    animation: none;
    opacity: 0.62;
  }
}
</style>
