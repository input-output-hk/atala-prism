import React from 'react';
import { Form, Input, InputNumber, Row } from 'antd';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { DownOutlined } from '@ant-design/icons';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';
import {
  DEFAULT_WIDTH_INPUT,
  IMPORT_CONTACTS,
  CREDENTIAL_TYPE_FIELD_TYPES
} from '../../../helpers/constants';
import { columnShape, importUseCasePropType, skeletonShape } from '../../../helpers/propShapes';
import CustomDatePicker from '../../common/Atoms/CustomDatePicker/CustomDatePicker';
import './_style.scss';

const { INT, DATE } = CREDENTIAL_TYPE_FIELD_TYPES;

const IndividualForm = ({ field, skeleton, columns, onRemove, useCase }) => {
  const { t } = useTranslation();

  const getWidth = item =>
    columns.find(col => col.fieldKey === item.fieldKey)?.width || DEFAULT_WIDTH_INPUT;

  const getWidthStyle = item => ({ width: getWidth(item) });

  const getInputByType = ({ type, placeholder, editable }) => {
    if (type === INT)
      return <InputNumber type="number" placeholder={placeholder} disabled={!editable} />;
    if (type === DATE) {
      return (
        <CustomDatePicker
          placeholder={placeholder}
          suffixIcon={<DownOutlined />}
          disabled={!editable}
        />
      );
    }
    return <Input placeholder={placeholder} disabled={!editable} />;
  };

  return (
    <div key={field.key} className="IndividualFormRow">
      {skeleton.map(item => (
        <Form.Item
          {...field}
          className="IndividualFormCol"
          style={getWidthStyle(item)}
          name={[field.key, item.name]}
          fieldKey={[field.key, item.fieldKey]}
          rules={item.rules}
          key={item.fieldKey}
        >
          {getInputByType(item)}
        </Form.Item>
      ))}
      {useCase === IMPORT_CONTACTS && (
        <Row className="IndividualFormCol">
          <CustomButton
            buttonProps={{
              onClick: () => onRemove(field.name),
              className: 'theme-link'
            }}
            buttonText={t('actions.delete')}
          />
        </Row>
      )}
    </div>
  );
};

IndividualForm.defaultProps = {};

IndividualForm.propTypes = {
  field: PropTypes.shape({
    name: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    key: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
  }).isRequired,
  columns: columnShape.isRequired,
  onRemove: PropTypes.func.isRequired,
  skeleton: skeletonShape.isRequired,
  useCase: importUseCasePropType.isRequired
};

export default IndividualForm;
