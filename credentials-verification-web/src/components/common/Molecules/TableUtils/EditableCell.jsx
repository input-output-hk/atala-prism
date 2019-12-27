import React, { useState } from 'react';
import { Input, Form, DatePicker } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { noEmptyInput } from '../../../../helpers/formRules';

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
        return <DatePicker {...props} />;
      default:
        return <Input {...props} />;
    }
  };

  const renderCell = form => {
    setCellForm(form);

    return editing ? (
      <Form.Item>
        {cellForm.getFieldDecorator(dataIndex, {
          rules: [noEmptyInput(t('errors.form.emptyField'))],
          initialValue: record[dataIndex]
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
