import React, { useState } from 'react';
import { Input, Form, DatePicker } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import moment from 'moment';
import { noEmptyInput, futureDate } from '../../../../helpers/formRules';
import { simpleMomentFormatter } from '../../../../helpers/formatters';

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

      let dateToSave;
      if (type === 'date' && values[id]) {
        const dateAsString = simpleMomentFormatter(values[id]);
        dateToSave = { [id]: dateAsString };
      }

      toggleEdit();
      const toSave = Object.assign({}, record, values, dateToSave);

      handleSave(toSave);
    });
  };

  const save = ({ currentTarget: { id } }) => {
    cellForm.validateFields((errors, values) => {
      if (errors && errors[id]) {
        return;
      }

      toggleEdit();
      const toSave = Object.assign({}, record, values);

      handleSave(toSave);
    });
  };

  const renderChild = (child, form) => {
    const [, , content] = child;

    if (content === '' || content === null) {
      setCellForm(form);
      setEditing(true);
    }

    return child;
  };

  const getElement = () => {
    const props = { onPressEnter: save, onBlur: save };

    switch (type) {
      case 'date':
        return <DatePicker onPressEnter={saveDate} onBlur={saveDate} />;
      default:
        return <Input onPressEnter={save} onBlur={save} />;
    }
  };

  const renderCell = form => {
    setCellForm(form);

    const savedData = record[dataIndex];

    const initialValue = savedData && isDate(type) ? moment(savedData) : savedData;

    const emptyRule = [noEmptyInput(t('errors.form.emptyField'))];
    const rulesByType = isDate(type)
      ? emptyRule.concat({
          validator: (_, value, cb) => futureDate(value, cb, moment.now()),
          message: t('newCredential.form.errors.futureError')
        })
      : emptyRule;

    return editing ? (
      <Form.Item>
        {cellForm.getFieldDecorator(dataIndex, {
          rules: rulesByType,
          initialValue
        })(getElement())}
      </Form.Item>
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
