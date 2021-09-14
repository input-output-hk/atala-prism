import { COMPACT_WIDTH_INPUT, VALIDATION_KEYS } from '../constants';
import { futureDate, pastDate } from '../formRules';

const { REQUIRED, FUTURE_DATE, PAST_DATE } = VALIDATION_KEYS;
export const getCredentialFormColumns = columns =>
  columns.map(({ title, dataIndex, editable, width, fixed, type }) => ({
    label: title,
    fieldKey: dataIndex,
    width: width || COMPACT_WIDTH_INPUT,
    fixed,
    editable,
    type
  }));

export const getCredentialFormSkeleton = fields =>
  fields.map(f => ({
    ...f,
    name: f.dataIndex,
    placeholder: f.title,
    fieldKey: f.dataIndex,
    rules: [...f.validations].map(validationKey => getRuleFromValidationKey(validationKey, f))
  }));

const getRuleFromValidationKey = (validationKey, field) => {
  if (validationKey === REQUIRED) return { required: true, message: `${field.title} is required.` };
  if (validationKey === FUTURE_DATE)
    return { validator: futureDate, message: `${field.title} must be a future date.` };
  if (validationKey === PAST_DATE)
    return { validator: pastDate, message: `${field.title} must be a past date.` };
  return {};
};
