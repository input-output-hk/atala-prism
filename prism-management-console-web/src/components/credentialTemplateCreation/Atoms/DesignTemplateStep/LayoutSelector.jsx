import React from 'react';
import { useTranslation } from 'react-i18next';
import { Form, Col, Radio } from 'antd';
import { templateLayouts } from '../../../../helpers/templateLayouts/templates';
import { useTemplateContext } from '../../../providers/TemplateContext';
import './_style.scss';

const LayoutSelector = () => {
  const { t } = useTranslation();
  const { templateSettings } = useTemplateContext();

  return (
    <Form.Item
      name="layout"
      label={t('credentialTemplateCreation.step2.style.layout')}
      rules={[
        {
          required: true
        }
      ]}
    >
      <Radio.Group>
        {templateLayouts.map((l, idx) => (
          <Radio value={idx}>
            <Col
              className={`LayoutOption shadow ${templateSettings.layout === idx ? 'selected' : ''}`}
            >
              <img className="img-logo" src={l.thumb} alt={`LayoutTemplate_${idx}`} />
            </Col>
          </Radio>
        ))}
      </Radio.Group>
    </Form.Item>
  );
};

LayoutSelector.propTypes = {};

export default LayoutSelector;
