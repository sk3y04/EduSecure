<template>
  <section class="page-hero desktop-page-panel hero-shell">
    <div class="max-w-4xl">
      <p class="section-kicker">Account security</p>
      <h2 class="section-title">Manage optional TOTP MFA</h2>
      <p class="section-copy max-w-none">
        Review MFA status, generate enrollment material, complete the first-code verification flow,
        and disable MFA only through the stronger password-plus-code requirement. The desktop layout
        keeps status, setup, and recovery guidance visible in one balanced security workspace.
      </p>
    </div>

    <div class="mt-8 grid gap-4 xl:grid-cols-3">
      <div class="surface-panel-muted px-5 py-4">
        <p class="meta-label">Security status</p>
        <p class="mt-2 text-base font-semibold text-[var(--color-heading)]">Current MFA posture stays visible</p>
      </div>
      <div class="surface-panel-muted px-5 py-4">
        <p class="meta-label">Enrollment flow</p>
        <p class="mt-2 text-base font-semibold text-[var(--color-heading)]">Setup, verification, and recovery remain grouped</p>
      </div>
      <div class="surface-panel-muted px-5 py-4">
        <p class="meta-label">Safer downgrade path</p>
        <p class="mt-2 text-base font-semibold text-[var(--color-heading)]">Disabling MFA still requires password and code proof</p>
      </div>
    </div>
  </section>
</template>

