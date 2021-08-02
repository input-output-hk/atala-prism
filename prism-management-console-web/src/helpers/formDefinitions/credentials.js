const fixedFields = ['contactName', 'externalId'];

export const CREDENTIAL_FORM_COLUMNS = columns =>
  columns.map(({ title, dataIndex }) => ({
    label: title,
    fieldKey: dataIndex,
    width: 400
  }));

export const CREDENTIAL_FORM = fields =>
  fields.map(f => ({
    name: f.dataIndex,
    placeholder: f.title,
    fieldKey: f.dataIndex,
    rules: [{ required: true, message: `${f.title} is required.` }],
    editable: !fixedFields.includes(f.dataIndex)
  }));
