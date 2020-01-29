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

  const [editing, setEditing] = useState(false);
  const [cellForm, setCellForm] = useState();

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

      handleSave(toSave);
    });
  };

  const save = ({ currentTarget: { id } }) => {
    cellForm.validateFields((errors, values) => {
      toggleEdit();
      const toSave = Object.assign({}, record, values);

      handleSave(toSave);
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

  const getError = () => {
    const data = record[dataIndex];

    if (!data) return `* ${t('errors.form.emptyField')}`;
    if (isDate(type) && moment(data).isSameOrAfter(moment()))
      return `* ${t('newCredential.form.errors.futureError')}`;
  };

  const renderCell = form => {
    setCellForm(form);

    const savedData = record[dataIndex];

    const emptyRule = [noEmptyInput(`Please add a ${dataIndex}`)];
    const futureRule = [
      {
        validator: (_, value, cb) => futureDate(value, cb, moment.now()),
        message: ''
      }
    ];
    const rulesByType = isDate(type) ? futureRule : emptyRule;

    const errorToShow = getError();

    return editing ? (
      <div className={errorToShow ? 'InputWithError' : ''}>
        <Form.Item>
          {errorToShow}
          {cellForm.getFieldDecorator(dataIndex, {
            rules: rulesByType,
            initialValue: savedData
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
