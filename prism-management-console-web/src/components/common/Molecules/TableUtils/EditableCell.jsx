import React, { useContext, useRef } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { Input, Form, DatePicker } from 'antd';
import { useTranslation } from 'react-i18next';
import { futureDate, generateRequiredRule, pastDate } from '../../../../helpers/formRules';

import './editableCell.scss';

const EditableCell = ({
  EditableContext,
  editable,
  children,
  dataIndex,
  record,
  handleSave,
  type,
  validations,
  ...restProps
}) => {
  const { t } = useTranslation();
  const inputRef = useRef(null);
  const form = useContext(EditableContext);

  const isDate = type === 'date';
  const savedData = record ? record[dataIndex] : '';

  const allRules = {
    required: generateRequiredRule(isDate, dataIndex),
    futureDate: {
      validator: (_rule, value, cb) => futureDate(value, cb, moment.now()),
      message: t('manualImport.table.futureDateRequirement', {
        field: t(`contacts.table.columns.${dataIndex}`)
      })
    },
    pastDate: {
      validator: (_rule, value, cb) => pastDate(value, cb, moment.now()),
      message: t('manualImport.table.pastDateRequirement', {
        field: t(`contacts.table.columns.${dataIndex}`)
      })
    }
  };

  const getFieldRules = validations.map(validationKey => allRules[validationKey]).filter(Boolean);

  const save = async () => {
    try {
      const values = await form.validateFields();
      const toSave = Object.assign(record, values, { errorFields: null });
      handleSave(toSave);
    } catch ({ errorFields, values }) {
      const toSave = Object.assign(record, values, { errorFields });
      handleSave(toSave);
    }
  };

  const getError = () => record?.errorFields?.find(({ name }) => name[0] === dataIndex)?.errors[0];

  const errorToShow = getError();

  return (
    <td {...restProps}>
      {editable ? (
        <Form.Item
          initialValue={savedData}
          rules={getFieldRules}
          validateFirst
          validateStatus={errorToShow ? 'error' : null}
          hasFeedback
          help={errorToShow}
          name={dataIndex}
        >
          {type === 'date' ? (
            <DatePicker ref={inputRef} onPressEnter={save} onBlur={save} onChange={save} />
          ) : (
            <Input ref={inputRef} onPressEnter={save} onBlur={save} />
          )}
        </Form.Item>
      ) : (
        children
      )}
    </td>
  );
};

EditableCell.defaultProps = {
  record: null,
  dataIndex: '',
  editable: false,
  validations: []
};

EditableCell.propTypes = {
  EditableContext: PropTypes.element.isRequired,
  record: PropTypes.shape({}),
  children: PropTypes.oneOf([PropTypes.bool, PropTypes.string]).isRequired,
  dataIndex: PropTypes.string,
  type: PropTypes.string.isRequired,
  editable: PropTypes.bool,
  handleSave: PropTypes.func.isRequired,
  validations: PropTypes.arrayOf(PropTypes.string)
};

export default EditableCell;
