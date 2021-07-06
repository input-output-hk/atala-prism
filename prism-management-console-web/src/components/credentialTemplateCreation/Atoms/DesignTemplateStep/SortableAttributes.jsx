import React from 'react';
import PropTypes from 'prop-types';
import { Button, Space } from 'antd';
import { DeleteOutlined, SwapOutlined } from '@ant-design/icons';
import { SortableContainer, SortableElement, SortableHandle } from 'react-sortable-hoc';
import FixedTextInput from './FixedTextInput';
import DynamicAttributeInput from './DynamicAttributeInput';

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

const SortableList = SortableContainer(({ children }) => <div className="row">{children}</div>);

const SortableAttributes = ({ attributes, move, remove }) => {
  const onSortEnd = ({ oldIndex, newIndex }) => move(oldIndex, newIndex);
  return (
    <SortableList onSortEnd={onSortEnd} useDragHandle>
      {attributes.map((value, index) => (
        <SortableItem key={`item-${value}`} index={index} value={value} remove={remove} />
      ))}
    </SortableList>
  );
};

SortableAttributes.propTypes = {
  attributes: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
      fieldKey: PropTypes.string.isRequired
    })
  ).isRequired,
  move: PropTypes.func.isRequired,
  remove: PropTypes.func.isRequired
};

export default SortableAttributes;
