import React from 'react';
import PropTypes from 'prop-types';
import { SortableContainer } from 'react-sortable-hoc';
import SortableItem from './SortableItem';
import { templateBodyAttributeShape } from '../../../../helpers/propShapes';

const SortableList = SortableContainer(({ children }) => <div className="row">{children}</div>);

const SortableAttributes = ({ attributes, move, remove }) => {
  const onSortEnd = ({ oldIndex, newIndex }) => move(oldIndex, newIndex);
  return (
    <SortableList onSortEnd={onSortEnd} useDragHandle>
      {attributes.map((value, index) => (
        <SortableItem key={`item-${value.key}`} index={index} value={value} remove={remove} />
      ))}
    </SortableList>
  );
};

SortableAttributes.propTypes = {
  attributes: PropTypes.arrayOf(templateBodyAttributeShape).isRequired,
  move: PropTypes.func.isRequired,
  remove: PropTypes.func.isRequired
};

export default SortableAttributes;
