import React from 'react';
import { useTranslation } from 'react-i18next';
import { Form } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import {
  getNewDynamicAttribute,
  getNewFixedTextAttribute
} from '../../../../hooks/useTemplateSettings';
import SortableAttributes from './SortableAttributes';
import { useTemplateContext } from '../../../providers/TemplateContext';

const BodyEditor = () => {
  const { t } = useTranslation();
  const { form, templateSettings } = useTemplateContext();
  const formValues = form.getFieldsValue();

  const currentConfig = { ...templateSettings, ...formValues };

  return (
    <Form.List name="credentialBody">
      {(attributes, { add, move, remove }) => {
        const attributesWithValues = currentConfig.credentialBody;

        const mergedAttributes = attributes.map((attr, index) => ({
          ...attr,
          ...attributesWithValues[index]
        }));

        return (
          <div>
            <div className="rowHeader">
              <h3>{t('credentialTemplateCreation.step2.content.body')}</h3>
              <div>
                <CustomButton
                  buttonText={t('credentialTemplateCreation.step2.content.addAttribute')}
                  buttonProps={{
                    className: 'theme-link',
                    icon: <PlusOutlined />,
                    onClick: () => add(getNewDynamicAttribute(mergedAttributes))
                  }}
                />
                <CustomButton
                  buttonText={t('credentialTemplateCreation.step2.content.addFixedText')}
                  buttonProps={{
                    className: 'theme-link',
                    icon: <PlusOutlined />,
                    onClick: () => add(getNewFixedTextAttribute(mergedAttributes))
                  }}
                />
              </div>
            </div>
            <div className="bodyItemContainer">
              <div className="">
                <SortableAttributes attributes={mergedAttributes} move={move} remove={remove} />
              </div>
            </div>
          </div>
        );
      }}
    </Form.List>
  );
};

BodyEditor.propTypes = {};

export default BodyEditor;
