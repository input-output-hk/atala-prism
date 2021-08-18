import React from 'react';
import { Form, Input, Row } from 'antd';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';
import { DEFAULT_WIDTH_INPUT, IMPORT_CONTACTS } from '../../../helpers/constants';
import { columnShape, importUseCasePropType, skeletonShape } from '../../../helpers/propShapes';

import './_style.scss';

const IndividualForm = ({ field, skeleton, columns, onRemove, useCase }) => {
  const { t } = useTranslation();

  const getWidth = item =>
    columns.find(col => col.fieldKey === item.fieldKey)?.width || DEFAULT_WIDTH_INPUT;

  const getWidthStyle = item => ({ width: getWidth(item) });

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
          <Input placeholder={item.placeholder} disabled={!item.editable} />
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
    name: PropTypes.string,
    key: PropTypes.oneOf([PropTypes.string, PropTypes.number])
  }).isRequired,
  columns: columnShape.isRequired,
  onRemove: PropTypes.func.isRequired,
  skeleton: skeletonShape.isRequired,
  useCase: importUseCasePropType.isRequired
};

export default IndividualForm;
