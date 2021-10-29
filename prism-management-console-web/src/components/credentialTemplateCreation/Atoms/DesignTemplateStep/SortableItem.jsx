import React from 'react';
import PropTypes from 'prop-types';
import { Button, Space } from 'antd';
import { DeleteOutlined, SwapOutlined } from '@ant-design/icons';
import { SortableElement, SortableHandle } from 'react-sortable-hoc';
import FixedTextInput from './FixedTextInput';
import DynamicAttributeInput from './DynamicAttributeInput';
import { templateBodyAttributeShape } from '../../../../helpers/propShapes';

const DragHandle = SortableHandle(() => (
  <div className="dragHandleWrapper">
    <SwapOutlined rotate={90} />
  </div>
));

const SortableItem = SortableElement(({ value, remove }) => {
  const { key, name, isFixedText } = value;
  return (
    <div className="sortable">
      <DragHandle />
      <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
        <div className="firstGroupInputContainer">
          {isFixedText ? <FixedTextInput value={value} /> : <DynamicAttributeInput value={value} />}
          <Button icon={<DeleteOutlined />} onClick={() => remove(name)} />
        </div>
      </Space>
    </div>
  );
});

SortableItem.propTypes = {
  value: templateBodyAttributeShape.isRequired,
  remove: PropTypes.func.isRequired
};

export default SortableItem;
