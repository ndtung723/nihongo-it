import * as yup from "yup";

export const loginSchema = yup.object({
  email: yup.string().email("Email không hợp lệ").required("Email là bắt buộc"),
  password: yup
    .string()
    .min(6, "Mật khẩu tối thiểu 6 ký tự")
    .required("Mật khẩu là bắt buộc"),
});

export const registerSchema = yup.object({
  email: yup.string().email("Email không hợp lệ").required("Email là bắt buộc"),
  fullName: yup
    .string()
    .min(2, "Tên tối thiểu 2 ký tự")
    .required("Họ tên là bắt buộc"),
  password: yup
    .string()
    .min(8, "Mật khẩu tối thiểu 8 ký tự")
    .matches(/[A-Z]/, "Phải có ít nhất 1 chữ hoa")
    .matches(/[0-9]/, "Phải có ít nhất 1 chữ số")
    .required("Mật khẩu là bắt buộc"),
  confirmPassword: yup
    .string()
    .oneOf([yup.ref("password")], "Mật khẩu xác nhận không khớp")
    .required("Xác nhận mật khẩu là bắt buộc"),
});

export const changePasswordSchema = yup.object({
  currentPassword: yup.string().required("Mật khẩu hiện tại là bắt buộc"),
  newPassword: yup
    .string()
    .min(8, "Mật khẩu tối thiểu 8 ký tự")
    .matches(/[A-Z]/, "Phải có ít nhất 1 chữ hoa")
    .matches(/[0-9]/, "Phải có ít nhất 1 chữ số")
    .required("Mật khẩu mới là bắt buộc"),
  confirmNewPassword: yup
    .string()
    .oneOf([yup.ref("newPassword")], "Mật khẩu xác nhận không khớp")
    .required("Xác nhận mật khẩu là bắt buộc"),
});

export const forgotPasswordSchema = yup.object({
  email: yup.string().email("Email không hợp lệ").required("Email là bắt buộc"),
});

export const resetPasswordSchema = yup.object({
  newPassword: yup
    .string()
    .min(8, "Mật khẩu tối thiểu 8 ký tự")
    .required("Mật khẩu mới là bắt buộc"),
  confirmPassword: yup
    .string()
    .oneOf([yup.ref("newPassword")], "Mật khẩu xác nhận không khớp")
    .required("Xác nhận mật khẩu là bắt buộc"),
});

export type LoginFormValues = yup.InferType<typeof loginSchema>;
export type RegisterFormValues = yup.InferType<typeof registerSchema>;
export type ChangePasswordFormValues = yup.InferType<
  typeof changePasswordSchema
>;
