import React from 'react';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import { Form } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import {
  getNewDynamicAttribute,
  getNewFixedTextAttribute
} from '../../../../helpers/templateHelpers';
import SortableAttributes from './SortableAttributes';
import { useTemplateSketch } from '../../../../hooks/useTemplateSketch';

const BodyEditor = observer(() => {
  const { t } = useTranslation();
  const { templateSketch } = useTemplateSketch();

  return (
    <Form.List
      name="credentialBody"
      rules={[
        {
          required: true,
          message: t('credentialTemplateCreation.errors.fieldIsRequired', {
            field: t('credentialTemplateCreation.step2.content.body')
          })
        }
      ]}
    >
      {(attributes, { add, move, remove }) => {
        const attributesWithValues = templateSketch.credentialBody;

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
});

export default BodyEditor;
