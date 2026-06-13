import { InputHTMLAttributes, forwardRef } from 'react'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  icon?: string
}

const FormInput = forwardRef<HTMLInputElement, Props>(
  ({ label, error, icon, ...rest }, ref) => (
    <div className="space-y-1">
      <label className="block text-sm font-medium text-white/70">{label}</label>
      <div className="relative">
        {icon && (
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40 text-lg pointer-events-none">
            {icon}
          </span>
        )}
        <input
          ref={ref}
          className={`input-field ${icon ? 'pl-10' : ''}`}
          {...rest}
        />
      </div>
      {error && <p className="error-text">{error}</p>}
    </div>
  )
)

FormInput.displayName = 'FormInput'
export default FormInput

