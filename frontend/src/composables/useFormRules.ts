import type { FormItemRule } from 'element-plus'

/**
 * Reusable form validation rules for Element Plus forms.
 * Usage:
 *   const { rules } = useFormRules()
 *   const formRules = {
 *     title: [rules.required('请输入标题'), rules.maxLength(200)],
 *     content: [rules.required('请输入内容'), rules.maxLength(10000)],
 *   }
 */
export function useFormRules() {
  const required = (message: string): FormItemRule => ({
    required: true,
    message,
    trigger: 'blur',
  })

  const maxLength = (max: number, message?: string): FormItemRule => ({
    max,
    message: message || `长度不能超过 ${max} 个字符`,
    trigger: 'blur',
  })

  const minLength = (min: number, message?: string): FormItemRule => ({
    min,
    message: message || `长度不能少于 ${min} 个字符`,
    trigger: 'blur',
  })

  const email = (message?: string): FormItemRule => ({
    type: 'email',
    message: message || '请输入有效的邮箱地址',
    trigger: 'blur',
  })

  const pattern = (regex: RegExp, message: string): FormItemRule => ({
    pattern: regex,
    message,
    trigger: 'blur',
  })

  const range = (min: number, max: number, message?: string): FormItemRule => ({
    type: 'number',
    min,
    max,
    message: message || `请输入 ${min} 到 ${max} 之间的数值`,
    trigger: 'blur',
  })

  const username: FormItemRule[] = [
    required('请输入用户名'),
    minLength(2, '用户名至少 2 个字符'),
    maxLength(32, '用户名不能超过 32 个字符'),
    pattern(/^[a-zA-Z0-9_一-龥]+$/, '用户名只能包含字母、数字、下划线和中文'),
  ]

  const password: FormItemRule[] = [
    required('请输入密码'),
    minLength(6, '密码至少 6 个字符'),
    maxLength(64, '密码不能超过 64 个字符'),
  ]

  const nickname: FormItemRule[] = [
    required('请输入昵称'),
    maxLength(32, '昵称不能超过 32 个字符'),
  ]

  const title: FormItemRule[] = [
    required('请输入标题'),
    maxLength(200, '标题不能超过 200 个字符'),
  ]

  const content: FormItemRule[] = [
    required('请输入内容'),
    maxLength(10000, '内容不能超过 10000 个字符'),
  ]

  return {
    rules: { required, maxLength, minLength, email, pattern, range },
    presets: { username, password, nickname, title, content },
  }
}
