import { z } from 'zod'

const JLPT_LEVELS = ['N1', 'N2', 'N3', 'N4', 'N5'] as const

// displayOrder is registered with `valueAsNumber: true` so it arrives as a
// number (NaN when empty). Accept NaN then transform to undefined for clean
// optional payload.
const optionalNonNegativeInt = z
  .number()
  .int()
  .min(0)
  .or(z.nan())
  .transform((v) => (Number.isNaN(v) ? undefined : v))
  .optional()

export const categorySchema = z.object({
  name: z.string().min(1, 'Vui lòng nhập tên'),
  meaning: z.string().min(1, 'Vui lòng nhập nghĩa'),
  description: z.string().optional(),
  displayOrder: optionalNonNegativeInt,
})
export type CategoryInput = z.infer<typeof categorySchema>

export const topicSchema = z.object({
  name: z.string().min(1, 'Vui lòng nhập tên'),
  meaning: z.string().min(1, 'Vui lòng nhập nghĩa'),
  description: z.string().optional(),
  displayOrder: optionalNonNegativeInt,
  categoryId: z.string().min(1, 'Vui lòng chọn danh mục'),
})
export type TopicInput = z.infer<typeof topicSchema>

export const vocabularySchema = z.object({
  term: z.string().min(1, 'Vui lòng nhập từ vựng'),
  meaning: z.string().min(1, 'Vui lòng nhập nghĩa'),
  pronunciation: z.string().optional(),
  example: z.string().optional(),
  exampleMeaning: z.string().optional(),
  audioPath: z.string().optional(),
  topicName: z.string().min(1, 'Vui lòng chọn chủ đề'),
  jlptLevel: z.enum(JLPT_LEVELS),
})
export type VocabularyInput = z.infer<typeof vocabularySchema>
