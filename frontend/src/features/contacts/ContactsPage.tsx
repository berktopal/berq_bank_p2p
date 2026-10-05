import { useState } from 'react'
import { Pencil, Search, Send, Trash2, UserPlus, Users } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { Field, Input } from '@/components/ui/Field'
import { Avatar, Card, EmptyState, PageHeader, Skeleton } from '@/components/ui/primitives'
import { Spinner } from '@/components/ui/Spinner'
import { useAccountLookup } from '@/features/accounts/api'
import { IbanInput } from '@/features/transfer/IbanInput'
import { errorMessage } from '@/lib/errors'
import { formatIban, isTrIbanFormat } from '@/lib/iban'
import type { Contact } from '@/lib/types'
import { useContacts, useCreateContact, useDeleteContact, useRenameContact } from './api'

export function ContactsPage() {
  const { t } = useTranslation()
  const contacts = useContacts()
  const [params, setParams] = useSearchParams()
  const [filter, setFilter] = useState('')
  const [editing, setEditing] = useState<Contact | null>(null)
  const [deleting, setDeleting] = useState<Contact | null>(null)
  const deleteContact = useDeleteContact()
  const addOpen = params.get('add') === '1'
  const setAddOpen = (open: boolean) =>
    setParams(
      (p) => {
        if (open) p.set('add', '1')
        else p.delete('add')
        return p
      },
      { replace: true },
    )

  const needle = filter.trim().toLocaleLowerCase('tr-TR')
  const visible = (contacts.data ?? []).filter(
    (c) =>
      !needle ||
      c.nickname.toLocaleLowerCase('tr-TR').includes(needle) ||
      c.ownerName.toLocaleLowerCase('tr-TR').includes(needle) ||
      c.iban.includes(needle.replace(/\s/g, '').toUpperCase()),
  )

  return (
    <>
      <PageHeader
        title={t('contacts.title')}
        subtitle={t('contacts.subtitle')}
        actions={
          <Button icon={<UserPlus className="size-4" />} onClick={() => setAddOpen(true)}>
            {t('contacts.add')}
          </Button>
        }
      />

      <Card className="p-4 sm:p-6">
        {(contacts.data?.length ?? 0) > 3 && (
          <div className="relative mb-4">
            <Search className="text-muted pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2" aria-hidden />
            <Input
              type="search"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder={t('contacts.filter')}
              aria-label={t('contacts.filter')}
              className="pl-10"
            />
          </div>
        )}

        {contacts.isPending ? (
          <div className="flex flex-col gap-4">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-14 w-full" />
            ))}
          </div>
        ) : contacts.isError ? (
          <EmptyState icon={<Users />} title={errorMessage(t, contacts.error)} />
        ) : contacts.data.length === 0 ? (
          <EmptyState
            icon={<Users />}
            title={t('contacts.empty')}
            hint={t('contacts.emptyHint')}
            action={
              <Button icon={<UserPlus className="size-4" />} onClick={() => setAddOpen(true)}>
                {t('contacts.add')}
              </Button>
            }
          />
        ) : (
          <ul className="divide-border divide-y">
            {visible.map((c) => (
              <li key={c.id} className="flex items-center gap-3 py-3">
                <Avatar name={c.nickname} />
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold">{c.nickname}</p>
                  <p className="text-muted truncate text-sm">
                    {c.ownerName} · <span className="tabular">{formatIban(c.iban)}</span>
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <ButtonLink to={`/app/transfer?to=${c.iban}`} size="sm" icon={<Send className="size-4" />}>
                    <span className="hidden sm:inline">{t('common.send')}</span>
                  </ButtonLink>
                  <button
                    type="button"
                    onClick={() => setEditing(c)}
                    className="text-muted hover:bg-surface-2 hover:text-text rounded-lg p-2"
                    aria-label={`${t('common.edit')}: ${c.nickname}`}
                  >
                    <Pencil className="size-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setDeleting(c)}
                    className="text-muted hover:bg-danger-soft hover:text-danger rounded-lg p-2"
                    aria-label={`${t('common.delete')}: ${c.nickname}`}
                  >
                    <Trash2 className="size-4" />
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <AddContactDialog open={addOpen} onClose={() => setAddOpen(false)} />
      <RenameContactDialog contact={editing} onClose={() => setEditing(null)} />
      <Dialog
        open={deleting !== null}
        onClose={() => setDeleting(null)}
        title={t('contacts.deleteTitle')}
        description={deleting ? t('contacts.deleteBody', { name: deleting.nickname }) : undefined}
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleting(null)}>
              {t('common.cancel')}
            </Button>
            <Button
              variant="danger"
              onClick={() => {
                if (!deleting) return
                deleteContact.mutate(deleting.id, {
                  onSuccess: () => toast.success(t('contacts.deleted')),
                  onError: (e) => toast.error(errorMessage(t, e)),
                })
                setDeleting(null)
              }}
            >
              {t('common.delete')}
            </Button>
          </>
        }
      />
    </>
  )
}

function AddContactDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useTranslation()
  const [nickname, setNickname] = useState('')
  const [iban, setIban] = useState('TR')
  const complete = isTrIbanFormat(iban)
  const lookup = useAccountLookup(open && complete ? iban : null)
  const create = useCreateContact()

  const close = () => {
    setNickname('')
    setIban('TR')
    create.reset()
    onClose()
  }

  return (
    <Dialog open={open} onClose={close} title={t('contacts.addTitle')}>
      <form
        noValidate
        className="flex flex-col gap-5"
        onSubmit={(e) => {
          e.preventDefault()
          if (!nickname.trim() || !lookup.data) return
          create.mutate(
            { nickname: nickname.trim(), iban },
            {
              onSuccess: () => {
                toast.success(t('contacts.added'))
                close()
              },
            },
          )
        }}
      >
        {create.isError && (
          <p role="alert" className="bg-danger-soft text-danger rounded-xl px-4 py-3 text-sm">
            {errorMessage(t, create.error)}
          </p>
        )}
        <Field label={t('contacts.iban')}>
          {({ id }) => (
            <>
              <IbanInput id={id} value={iban} onChange={setIban} placeholder={t('transfer.ibanPlaceholder')} aria-describedby="contact-iban-status" />
              <p id="contact-iban-status" aria-live="polite" className="min-h-5 text-sm">
                {complete && lookup.isFetching && <Spinner className="text-muted size-4" />}
                {complete && lookup.isError && <span className="text-danger">{errorMessage(t, lookup.error)}</span>}
                {complete && lookup.data && (
                  <span className="text-success font-medium">{t('transfer.recipientFound', { name: lookup.data.ownerName })}</span>
                )}
              </p>
            </>
          )}
        </Field>
        <Field label={t('contacts.nickname')}>
          {({ id }) => (
            <Input id={id} value={nickname} maxLength={40} onChange={(e) => setNickname(e.target.value)} placeholder={t('contacts.nicknamePlaceholder')} />
          )}
        </Field>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={close}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" loading={create.isPending} disabled={!lookup.data || !nickname.trim()}>
            {t('common.save')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}

function RenameContactDialog({ contact, onClose }: { contact: Contact | null; onClose: () => void }) {
  const { t } = useTranslation()
  const rename = useRenameContact()
  const [nickname, setNickname] = useState('')
  const [lastId, setLastId] = useState<number | null>(null)
  // Yeni bir alıcı düzenlenmeye başlandığında alanı onun adıyla doldur
  if (contact && contact.id !== lastId) {
    setLastId(contact.id)
    setNickname(contact.nickname)
  }
  return (
    <Dialog open={contact !== null} onClose={onClose} title={t('common.edit')}>
      <form
        className="flex flex-col gap-5"
        onSubmit={(e) => {
          e.preventDefault()
          if (!contact || !nickname.trim()) return
          rename.mutate(
            { id: contact.id, nickname: nickname.trim() },
            {
              onSuccess: () => {
                toast.success(t('contacts.renamed'))
                onClose()
              },
              onError: (err) => toast.error(errorMessage(t, err)),
            },
          )
        }}
      >
        <Field label={t('contacts.nickname')}>
          {({ id }) => <Input id={id} value={nickname} maxLength={40} onChange={(e) => setNickname(e.target.value)} />}
        </Field>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" loading={rename.isPending}>
            {t('common.save')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
