import React, { useState } from 'react';
import { Input, Form, DatePicker } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import moment from 'moment';
import { noEmptyInput, futureDate } from '../../../../helpers/formRules';

import './editableCell.scss';

const isDate = type => type === 'date';

const EditableCell = ({
  EditableContext: Consumer,
  record,
  handleSave,
  children,
  dataIndex,
  type,
  title,
  editable,
  ...restProps
}) => {
  const { t } = useTranslation();

  const [cellForm, setCellForm] = useState();
  const [editing, setEditing] = useState();

  const toggleEdit = () => {
    const isEditing = !editing;
    setEditing(isEditing);
  };

  const saveDate = ({ currentTarget: { id } }) => {
    cellForm.validateFields((errors, values) => {
      if (errors && !values[id]) {
        return;
      }

      toggleEdit();
      const toSave = Object.assign({}, record, values);

      handleSave({ ...toSave, errors });
    });
  };

  const save = () => {
    cellForm.validateFields((errors, values) => {
      toggleEdit();
      const toSave = Object.assign({}, record, values);
      handleSave({ ...toSave, errors });
    });
  };

  const renderChild = (child, form) => {
    const [, , content] = child;

    setEditing(true);
    if (content === '' || content === null) {
      setCellForm(form);
    }

    return child;
  };

  const getElement = () => {
    switch (type) {
      case 'date':
        return <DatePicker onPressEnter={saveDate} onBlur={saveDate} />;
      default:
        return <Input onPressEnter={save} onBlur={save} />;
    }
  };

  const getError = () =>
    record.errors && record.errors[dataIndex]?.errors?.map(({ message }) => message);

  const renderCell = form => {
    setCellForm(form);
    const savedData = record[dataIndex];

    const emptyRule = [
      noEmptyInput(
        t('manualImport.table.requirement', {
          requirement: t(`manualImport.table.${dataIndex}`)
        })
      )
    ];
    const futureRule = [
      {
        validator: (_, value, cb) => futureDate(value, cb, moment.now()),
        message: t('manualImport.table.requirement', {
          requirement: t('manualImport.table.dateRequirement')
        })
      }
    ];
    const rulesByType = isDate(type) ? futureRule : emptyRule;

    const errorToShow = getError();

    return editing ? (
      <div className="TableInputContainer">
        <Form.Item validateStatus={errorToShow ? 'error' : null} hasFeedback help={errorToShow}>
          {cellForm.getFieldDecorator(dataIndex, {
            initialValue: savedData,
            rules: rulesByType
          })(getElement())}
        </Form.Item>
      </div>
    ) : (
      <textbox onClick={toggleEdit}>{renderChild(children, form)}</textbox>
    );
  };

  return <td {...restProps}>{editable ? <Consumer>{renderCell}</Consumer> : children}</td>;
};

EditableCell.propTypes = {
  EditableContext: PropTypes.element.isRequired,
  record: PropTypes.shape.isRequired,
  children: PropTypes.arrayOf(PropTypes.element).isRequired,
  dataIndex: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired,
  editable: PropTypes.bool.isRequired,
  handleSave: PropTypes.func.isRequired
};

export default EditableCell;
