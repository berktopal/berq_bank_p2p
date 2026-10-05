import { useState } from 'react'
import { Pencil, PiggyBank, Plus, Trash2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { Field, Input, Select } from '@/components/ui/Field'
import { Card, EmptyState, PageHeader, Segmented, Skeleton } from '@/components/ui/primitives'
import { errorMessage } from '@/lib/errors'
import { formatNumber, parseAmount } from '@/lib/format'
import { CATEGORIES, type Budget, type Category, type Currency } from '@/lib/types'
import { useBudgets, useDeleteBudget, useUpsertBudget } from './api'
import { BudgetMeter } from './BudgetMeter'

// Birikim bir harcama değildir; bütçelenecek kategoriler
const BUDGETABLE = CATEGORIES.filter((c) => c !== 'SAVINGS')

export function BudgetsPage() {
  const { t } = useTranslation()
  const budgets = useBudgets()
  const remove = useDeleteBudget()
  const [editing, setEditing] = useState<Budget | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Budget | null>(null)

  return (
    <>
      <PageHeader
        title={t('budgets.title')}
        subtitle={t('budgets.subtitle')}
        actions={
          <Button icon={<Plus className="size-4" />} onClick={() => setEditing('new')}>
            {t('budgets.add')}
          </Button>
        }
      />

      {budgets.isPending ? (
        <div className="grid gap-4 md:grid-cols-2">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-32 rounded-2xl" />
          ))}
        </div>
      ) : budgets.isError ? (
        <Card>
          <EmptyState icon={<PiggyBank />} title={errorMessage(t, budgets.error)} />
        </Card>
      ) : budgets.data.length === 0 ? (
        <Card>
          <EmptyState
            icon={<PiggyBank />}
            title={t('budgets.empty')}
            hint={t('budgets.emptyHint')}
            action={<Button onClick={() => setEditing('new')}>{t('budgets.add')}</Button>}
          />
        </Card>
      ) : (
        <>
          <ul className="grid gap-4 md:grid-cols-2">
            {budgets.data.map((b) => (
              <li key={b.id}>
                <Card className="flex flex-col gap-3 p-5">
                  <BudgetMeter budget={b} />
                  <div className="flex justify-end gap-1">
                    <button
                      type="button"
                      onClick={() => setEditing(b)}
                      className="text-muted hover:bg-surface-2 hover:text-text rounded-lg p-2"
                      aria-label={`${t('budgets.edit')}: ${t(`categories.${b.category}`)}`}
                    >
                      <Pencil className="size-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeleting(b)}
                      className="text-muted hover:bg-danger-soft hover:text-danger rounded-lg p-2"
                      aria-label={`${t('common.delete')}: ${t(`categories.${b.category}`)}`}
                    >
                      <Trash2 className="size-4" />
                    </button>
                  </div>
                </Card>
              </li>
            ))}
          </ul>
          <p className="text-muted mt-4 text-sm">{t('budgets.note')}</p>
        </>
      )}

      <BudgetDialog budget={editing} onClose={() => setEditing(null)} />
      <Dialog
        open={deleting !== null}
        onClose={() => setDeleting(null)}
        title={deleting ? t('budgets.deleteConfirm', { category: t(`categories.${deleting.category}`) }) : ''}
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleting(null)}>
              {t('common.cancel')}
            </Button>
            <Button
              variant="danger"
              loading={remove.isPending}
              onClick={() =>
                deleting &&
                remove.mutate(deleting.id, {
                  onSuccess: () => {
                    toast.success(t('budgets.deleted'))
                    setDeleting(null)
                  },
                  onError: (e) => toast.error(errorMessage(t, e)),
                })
              }
            >
              {t('common.delete')}
            </Button>
          </>
        }
      />
    </>
  )
}

function BudgetDialog({ budget, onClose }: { budget: Budget | 'new' | null; onClose: () => void }) {
  const { t } = useTranslation()
  const upsert = useUpsertBudget()
  const existing = budget !== 'new' ? budget : null
  const [form, setForm] = useState<{ category: Category; currency: Currency; limit: string } | null>(null)
  // Diyalog her açılışta düzenlenen bütçeyle (ya da boş) başlar
  const key = budget === null ? null : existing ? `edit-${existing.id}` : 'new'
  const [openedFor, setOpenedFor] = useState<string | null>(null)
  if (key !== openedFor) {
    setOpenedFor(key)
    setForm(
      key === null
        ? null
        : {
            category: existing?.category ?? 'FOOD',
            currency: (existing?.currency as Currency | undefined) ?? 'TRY',
            limit: existing ? formatNumber(existing.monthlyLimit) : '',
          },
    )
  }
  const limit = form ? parseAmount(form.limit) : null
  const invalid = limit === null || limit < 1

  return (
    <Dialog open={budget !== null} onClose={onClose} title={existing ? t('budgets.edit') : t('budgets.add')}>
      {form && (
        <form
          noValidate
          className="flex flex-col gap-5"
          onSubmit={(e) => {
            e.preventDefault()
            if (invalid || limit === null) return
            upsert.mutate(
              { category: form.category, currency: form.currency, monthlyLimit: limit },
              {
                onSuccess: () => {
                  toast.success(t('budgets.saved'))
                  onClose()
                },
              },
            )
          }}
        >
          {upsert.isError && (
            <p role="alert" className="bg-danger-soft text-danger rounded-xl px-4 py-3 text-sm">
              {errorMessage(t, upsert.error)}
            </p>
          )}
          <Field label={t('budgets.category')}>
            {({ id }) => (
              <Select
                id={id}
                value={form.category}
                disabled={Boolean(existing)}
                onChange={(e) => setForm({ ...form, category: e.target.value as Category })}
              >
                {BUDGETABLE.map((c) => (
                  <option key={c} value={c}>
                    {t(`categories.${c}`)}
                  </option>
                ))}
              </Select>
            )}
          </Field>
          {!existing && (
            <div className="flex flex-col gap-1.5">
              <span className="text-sm font-medium">{t('budgets.currency')}</span>
              <Segmented
                label={t('budgets.currency')}
                value={form.currency}
                onChange={(currency) => setForm({ ...form, currency })}
                options={(['TRY', 'USD', 'EUR'] as const).map((c) => ({ value: c, label: c }))}
                className="self-start"
              />
            </div>
          )}
          <Field label={t('budgets.limit')}>
            {({ id }) => (
              <Input
                id={id}
                inputMode="decimal"
                placeholder="2.000"
                value={form.limit}
                onChange={(e) => setForm({ ...form, limit: e.target.value.replace(/[^\d.,]/g, '') })}
                className="tabular text-lg font-semibold"
              />
            )}
          </Field>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={onClose}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" disabled={invalid} loading={upsert.isPending}>
              {t('common.save')}
            </Button>
          </div>
        </form>
      )}
    </Dialog>
  )
}
