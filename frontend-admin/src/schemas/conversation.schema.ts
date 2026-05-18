import { z } from 'zod'

const JLPT_LEVELS = ['N1', 'N2', 'N3', 'N4', 'N5'] as const

const optionalPositiveInt = z
  .number()
  .int()
  .min(0)
  .or(z.nan())
  .transform((v) => (Number.isNaN(v) ? undefined : v))
  .optional()

export const conversationSchema = z.object({
  title: z.string().min(1, 'Vui lòng nhập tiêu đề'),
  description: z.string().optional(),
  jlptLevel: z.enum(JLPT_LEVELS).optional(),
  unit: optionalPositiveInt,
})
export type ConversationInput = z.infer<typeof conversationSchema>
