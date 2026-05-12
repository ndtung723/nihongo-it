import * as yup from "yup";

export const flashcardSchema = yup.object({
  frontText: yup
    .string()
    .max(500, "Mặt trước tối đa 500 ký tự")
    .required("Mặt trước là bắt buộc"),
  backText: yup
    .string()
    .max(500, "Mặt sau tối đa 500 ký tự")
    .required("Mặt sau là bắt buộc"),
  reading: yup.string().max(200, "Phiên âm tối đa 200 ký tự").optional(),
  exampleSentence: yup
    .string()
    .max(1000, "Câu ví dụ tối đa 1000 ký tự")
    .optional(),
});

export type FlashcardFormValues = yup.InferType<typeof flashcardSchema>;
