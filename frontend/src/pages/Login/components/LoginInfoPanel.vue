<template>
  <section class="page-hero lg:p-[clamp(2.5rem,6vw,5rem)]">
    <p class="section-kicker">EduSecure portal</p>
    <h1 class="hero-title mt-4 max-w-4xl">Security-focused access for coursework operations.</h1>
    <p class="section-copy max-w-2xl">
      Sign in to reach the protected assignment and submission workspace. The interface keeps the
      security journey direct: authenticate, complete MFA if required, and review integrity data
      without decorative visual noise.
    </p>

    <div class="mt-10 grid gap-4 lg:grid-cols-[minmax(0,1.25fr)_minmax(18rem,0.75fr)] lg:items-start">
      <article class="surface-panel-muted p-6">
        <p class="text-lg font-semibold text-[var(--color-heading)]">What the sign-in flow preserves</p>
        <p class="mt-3 text-base leading-7 text-[var(--color-text-soft)]">
          Successful authentication establishes the secure cookie used for the protected app,
          while second-factor checks and role boundaries stay enforced by backend policy rather than
          decorative UI cues.
        </p>
      </article>

      <div class="grid gap-4">
        <article class="info-card">
          <p class="text-base font-semibold text-[var(--color-heading)]">MFA-aware sign-in</p>
          <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
            Accounts with TOTP enabled are challenged before the authenticated session is created.
          </p>
        </article>
        <article class="info-card">
          <p class="text-base font-semibold text-[var(--color-heading)]">Role-based access</p>
          <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
            Student and lecturer flows stay separated by the backend role policy already in place.
          </p>
        </article>
      </div>
    </div>
  </section>
</template>

