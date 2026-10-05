import clsx, { type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** Koşullu sınıfları birleştirir; çakışan Tailwind sınıflarında sonuncusu kazanır (ör. w-full + w-auto → w-auto). */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}
