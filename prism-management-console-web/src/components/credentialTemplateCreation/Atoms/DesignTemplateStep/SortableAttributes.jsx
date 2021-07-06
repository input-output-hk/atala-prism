import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Button, Form, Input, Select, Space } from 'antd';
import { DeleteOutlined, MenuOutlined } from '@ant-design/icons';
import { SortableContainer, SortableElement, SortableHandle } from 'react-sortable-hoc';

const { Option } = Select;

const attributeTypeOptions = ['text', 'date', 'number'];

const DragHandle = SortableHandle(() => <MenuOutlined />);

const SortableItem = SortableElement(({ value: { key, name, fieldKey, ...restField }, remove }) => {
  const { t } = useTranslation();
  return (
    <div>
      <DragHandle />
      <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
        <div className="firstGroupInputContainer">
          <Form.Item
            className="inputContainer"
            {...restField}
            name={[name, 'attributeLabel']}
            fieldKey={[fieldKey, 'attributeLabel']}
            label={t('credentialTemplateCreation.step2.content.attributeLabel', {
              index: fieldKey + 1
            })}
            rules={[{ required: true }]}
          >
            <Input placeholder={`Attribute ${fieldKey}`} />
          </Form.Item>
          <Form.Item
            className="inputContainer"
            {...restField}
            name={[name, 'attributeType']}
            fieldKey={[fieldKey, 'attributeType']}
            label={t('credentialTemplateCreation.step2.content.attributeType')}
          >
            <Select>
              {attributeTypeOptions.map(option => (
                <Option value={option}>
                  {t(`credentialTemplateCreation.step2.content.attributeTypeOptions.${option}`)}
                </Option>
              ))}
            </Select>
          </Form.Item>
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
