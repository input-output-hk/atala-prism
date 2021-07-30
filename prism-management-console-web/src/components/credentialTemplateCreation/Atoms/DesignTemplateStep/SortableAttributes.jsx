import React from 'react';
import PropTypes from 'prop-types';
import { SortableContainer } from 'react-sortable-hoc';
import SortableItem from './SortableItem';

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
