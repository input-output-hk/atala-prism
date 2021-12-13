import React, { useContext, useRef } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { Input, Form, DatePicker } from 'antd';
import { useTranslation } from 'react-i18next';
import { futureDate, generateRequiredRule, pastDate } from '../../../../helpers/formRules';
import { humanizeCamelCaseString } from '../../../../helpers/genericHelpers';

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
  preExistingEntries,
  dataSource,
  ...restProps
}) => {
  const { t } = useTranslation();
  const inputRef = useRef(null);
  const form = useContext(EditableContext);

  const isDate = type === 'date';
  const savedData = record ? record[dataIndex] : '';

  const allRules = {
    required: generateRequiredRule(isDate, dataIndex),
    unique: {
      validator: (rule, value, cb) => {
        const shouldSendMessage = dataSource.some(
          row => row.key !== record.key && row[dataIndex] === value
        );
        const message = shouldSendMessage ? rule.message : undefined;
        cb(message);
      },
      message: t('manualImport.table.uniqueFieldRequirement', {
        field: t(`contacts.table.columns.${dataIndex}`, {
          defaultValue: humanizeCamelCaseString(dataIndex)
        })
      })
    },
    checkPreexisting: {
      validator: (rule, value, cb) => {
        if (preExistingEntries.some(row => row[dataIndex] === value)) cb(rule.message);
        else cb();
      },
      message: t('manualImport.table.checkPreexistingRequirement', {
        field: t(`contacts.table.columns.${dataIndex}`, {
          defaultValue: humanizeCamelCaseString(dataIndex)
        })
      })
    },
    futureDate: {
      validator: (_rule, value, cb) => futureDate(value, cb, moment.now()),
      message: t('manualImport.table.futureDateRequirement', {
        field: t(`contacts.table.columns.${dataIndex}`, {
          defaultValue: humanizeCamelCaseString(dataIndex)
        })
      })
    },
    pastDate: {
      validator: (_rule, value, cb) => pastDate(value, cb, moment.now()),
      message: t('manualImport.table.pastDateRequirement', {
        field: t(`contacts.table.columns.${dataIndex}`, {
          defaultValue: humanizeCamelCaseString(dataIndex)
        })
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

  const getElement = () => {
    if (type === 'date')
      return <DatePicker ref={inputRef} onPressEnter={save} onBlur={save} onChange={save} />;
    return <Input ref={inputRef} onPressEnter={save} onBlur={save} />;
  };

  const validateStatus = errorToShow ? 'error' : null;

  return (
    <td {...restProps}>
      {editable ? (
        <Form.Item
          initialValue={savedData}
          rules={getFieldRules}
          validateFirst
          validateStatus={validateStatus}
          hasFeedback
          help={errorToShow}
          name={dataIndex}
        >
          {getElement()}
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
  validations: [],
  dataSource: [],
  preExistingEntries: null
};

EditableCell.propTypes = {
  EditableContext: PropTypes.element.isRequired,
  record: PropTypes.shape({
    key: PropTypes.number,
    errorFields: PropTypes.arrayOfType(
      PropTypes.shape({
        name: PropTypes.string
      })
    )
  }),
  children: PropTypes.node.isRequired,
  dataIndex: PropTypes.string,
  type: PropTypes.string.isRequired,
  editable: PropTypes.bool,
  handleSave: PropTypes.func.isRequired,
  validations: PropTypes.arrayOf(PropTypes.string),
  dataSource: PropTypes.arrayOf(PropTypes.shape({ externalId: PropTypes.string })),
  preExistingEntries: PropTypes.arrayOf(PropTypes.shape({ externalId: PropTypes.string }))
};

export default EditableCell;
