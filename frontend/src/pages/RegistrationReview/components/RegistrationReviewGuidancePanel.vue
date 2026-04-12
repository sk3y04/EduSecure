<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Review guidance</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        Keep approval policy and decision expectations visible while working through the queue.
      </p>
    </div>

    <div class="space-y-4">
      <div class="surface-panel-muted px-4 py-4">
        <p class="meta-label">Role scope</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">
          Lecturers review requests for spaces they manage. Admins can review all pending requests.
        </p>
      </div>

      <div class="surface-panel-muted px-4 py-4">
        <p class="meta-label">Approve</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">
          Approval creates membership immediately and removes the item from the pending queue.
        </p>
      </div>

      <div class="surface-panel-muted px-4 py-4">
        <p class="meta-label">Reject</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">
          Rejection records an auditable outcome without adding the student to the space roster.
        </p>
      </div>
    </div>
  </section>
</template>

