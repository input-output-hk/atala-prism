import React from 'react';
import { useTranslation } from 'react-i18next';
import { Form } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { getDefaultAttribute } from '../../../../hooks/useTemplateSettings';
import SortableAttributes from './SortableAttributes';

const BodyEditor = () => {
  const { t } = useTranslation();
  return (
    <Form.List name="credentialBody">
      {(attributes, { add, move, remove }) => (
        <div>
          <div className="rowHeader">
            <h3>{t('credentialTemplateCreation.step2.content.body')}</h3>
            <div>
              <CustomButton
                buttonText="Add Attribute"
                buttonProps={{
                  className: 'theme-link',
                  icon: <PlusOutlined />,
                  onClick: () => add(getDefaultAttribute(attributes.length + 1))
                }}
              />
              <CustomButton
                buttonText="Add fixed text"
                buttonProps={{
                  className: 'theme-link',
                  icon: <PlusOutlined />
                }}
              />
            </div>
          </div>
          <div className="bodyItemContainer">
            <div className="">
              <SortableAttributes attributes={attributes} move={move} remove={remove} />
            </div>
          </div>
        </div>
      )}
    </Form.List>
  );
};

BodyEditor.propTypes = {};

export default BodyEditor;
