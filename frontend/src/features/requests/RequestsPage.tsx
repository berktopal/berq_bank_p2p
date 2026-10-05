import { useState } from 'react'
import { CheckCircle2, HandCoins, Plus, XCircle } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { Avatar, Badge, Card, EmptyState, PageHeader, Segmented, Skeleton } from '@/components/ui/primitives'
import { errorMessage } from '@/lib/errors'
import { formatDate, formatMoney } from '@/lib/format'
import { formatIban } from '@/lib/iban'
import type { PaymentRequest, PaymentRequestStatus } from '@/lib/types'
import { usePaymentRequests, useRespondRequest } from './api'
import { NewRequestDialog } from './NewRequestDialog'
import { PayRequestDialog } from './PayRequestDialog'

const STATUS_TONE: Record<PaymentRequestStatus, 'warning' | 'success' | 'danger' | 'neutral'> = {
  PENDING: 'warning',
  PAID: 'success',
  DECLINED: 'danger',
  CANCELLED: 'neutral',
  EXPIRED: 'neutral',
}

export function RequestsPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') === 'OUT' ? 'OUT' : 'IN'
  const setParam = (key: string, value: string | null) =>
    setParams(
      (p) => {
        if (value === null) p.delete(key)
        else p.set(key, value)
        return p
      },
      { replace: true },
    )
  const requests = usePaymentRequests(tab)
  const [paying, setPaying] = useState<PaymentRequest | null>(null)
  const [declining, setDeclining] = useState<PaymentRequest | null>(null)
  const decline = useRespondRequest('decline')
  const cancel = useRespondRequest('cancel')

  return (
    <>
      <PageHeader
        title={t('requests.title')}
        subtitle={t('requests.subtitle')}
        actions={
          <Button icon={<Plus className="size-4" />} onClick={() => setParam('new', '1')}>
            {t('requests.new')}
          </Button>
        }
      />

      <Segmented
        label={t('requests.title')}
        value={tab}
        onChange={(v) => setParam('tab', v)}
        options={[
          { value: 'IN', label: t('requests.incoming') },
          { value: 'OUT', label: t('requests.outgoing') },
        ]}
        className="mb-4"
      />

      <Card className="p-2 sm:p-4">
        {requests.isPending ? (
          <div className="flex flex-col gap-3 p-2">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-20 w-full" />
            ))}
          </div>
        ) : requests.isError ? (
          <EmptyState icon={<HandCoins />} title={errorMessage(t, requests.error)} />
        ) : requests.data.content.length === 0 ? (
          <EmptyState
            icon={<HandCoins />}
            title={tab === 'IN' ? t('requests.emptyIncoming') : t('requests.emptyOutgoing')}
            action={
              tab === 'OUT' && (
                <Button icon={<Plus className="size-4" />} onClick={() => setParam('new', '1')}>
                  {t('requests.new')}
                </Button>
              )
            }
          />
        ) : (
          <ul className="divide-border divide-y">
            {requests.data.content.map((r) => (
              <li key={r.id} className="flex flex-col gap-3 p-3 sm:flex-row sm:items-center">
                <div className="flex min-w-0 flex-1 items-center gap-3">
                  <Avatar name={r.counterpartyName} />
                  <div className="min-w-0">
                    <p className="truncate font-semibold">
                      {r.direction === 'INCOMING'
                        ? t('requests.from', { name: r.counterpartyName })
                        : t('requests.to', { name: r.counterpartyName })}
                    </p>
                    <p className="text-muted truncate text-sm">
                      {[r.description, formatDate(r.createdAt)].filter(Boolean).join(' · ')}
                      {r.status === 'PENDING' && <> · {t('requests.expires', { date: formatDate(r.expiresAt) })}</>}
                    </p>
                  </div>
                </div>
                <div className="flex items-center justify-between gap-3 sm:justify-end">
                  <div className="text-right">
                    <p className="tabular font-semibold">{formatMoney(r.amount, r.currency)}</p>
                    <Badge tone={STATUS_TONE[r.status]}>{t(`requests.status.${r.status}`)}</Badge>
                  </div>
                  {r.status === 'PENDING' && r.direction === 'INCOMING' && (
                    <div className="flex gap-2">
                      <Button size="sm" icon={<CheckCircle2 className="size-4" />} onClick={() => setPaying(r)}>
                        {t('requests.pay')}
                      </Button>
                      <Button size="sm" variant="secondary" icon={<XCircle className="size-4" />} onClick={() => setDeclining(r)}>
                        {t('requests.decline')}
                      </Button>
                    </div>
                  )}
                  {r.status === 'PENDING' && r.direction === 'OUTGOING' && (
                    <Button
                      size="sm"
                      variant="secondary"
                      loading={cancel.isPending && cancel.variables === r.id}
                      onClick={() =>
                        cancel.mutate(r.id, {
                          onSuccess: () => toast.success(t('requests.cancelled')),
                          onError: (e) => toast.error(errorMessage(t, e)),
                        })
                      }
                    >
                      {t('requests.cancel')}
                    </Button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <NewRequestDialog
        open={params.get('new') === '1'}
        onClose={() => setParam('new', null)}
        // Tek güncelleme: art arda iki setParams çağrısında ikincisi birincinin değişikliğini ezer
        onCreated={() =>
          setParams(
            (p) => {
              p.set('tab', 'OUT')
              p.delete('new')
              return p
            },
            { replace: true },
          )
        }
      />
      <PayRequestDialog request={paying} onClose={() => setPaying(null)} />
      <Dialog
        open={declining !== null}
        onClose={() => setDeclining(null)}
        title={t('requests.declineConfirm')}
        description={
          declining
            ? `${declining.counterpartyName} · ${formatMoney(declining.amount, declining.currency)} · ${formatIban(declining.requesterIban)}`
            : undefined
        }
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeclining(null)}>
              {t('common.cancel')}
            </Button>
            <Button
              variant="danger"
              loading={decline.isPending}
              onClick={() =>
                declining &&
                decline.mutate(declining.id, {
                  onSuccess: () => {
                    toast.success(t('requests.declined'))
                    setDeclining(null)
                  },
                  onError: (e) => toast.error(errorMessage(t, e)),
                })
              }
            >
              {t('requests.decline')}
            </Button>
          </>
        }
      />
    </>
  )
}
