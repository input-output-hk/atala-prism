import { COMPACT_WIDTH_INPUT } from '../constants';

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
    rules: [{ required: true, message: `${f.title} is required.` }]
  }));
