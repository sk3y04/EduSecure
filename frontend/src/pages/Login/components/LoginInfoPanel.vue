<template>
  <section class="surface-panel p-8 lg:p-10">
    <p class="section-kicker tracking-[0.3em]">EduSecure portal</p>
    <h1 class="section-title text-4xl">Security-focused access for coursework operations</h1>
    <p class="section-copy max-w-2xl text-base leading-7">
      Sign in to reach the protected assignment and submission workspace. The interface keeps the
      security journey direct: authenticate, complete MFA if required, and review integrity data
      without decorative visual noise.
    </p>

    <div class="mt-8 grid gap-4 md:grid-cols-3">
      <article class="info-card">
        <p class="text-sm font-semibold text-slate-900">Protected session</p>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          Successful authentication establishes the secure cookie used for the protected app.
        </p>
      </article>
      <article class="info-card">
        <p class="text-sm font-semibold text-slate-900">MFA-aware sign-in</p>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          Accounts with TOTP enabled are challenged before the authenticated session is created.
        </p>
      </article>
      <article class="info-card">
        <p class="text-sm font-semibold text-slate-900">Role-based access</p>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          Student and lecturer flows stay separated by the backend role policy already in place.
        </p>
      </article>
    </div>
  </section>
</template>

