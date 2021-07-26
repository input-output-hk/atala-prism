import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Form, Input, message } from 'antd';
import { exactValueExists } from '../../../../helpers/filterHelpers';
import { useDebounce } from '../../../../hooks/useDebounce';
import { withApi } from '../../../providers/withApi';
import { useCredentialTypes } from '../../../../hooks/useCredentialTypes';
import { credentialTypesManagerShape } from '../../../../helpers/propShapes';
import Logger from '../../../../helpers/Logger';

import './_style.scss';

const TemplateName = ({ api }) => {
  const { t } = useTranslation();
  const { getCredentialTypes } = useCredentialTypes(api.credentialTypesManager);

  const templateExists = (_rules, value, cb) =>
    getCredentialTypes()
      .then(credentialTypes => {
        if (exactValueExists(credentialTypes, value, 'name'))
          cb(t('credentialTemplateCreation.errors.preExisting', { value }));
        else cb();
      })
      .catch(error => {
        Logger.error('[CredentialTypes.getCredentialTypes] Error: ', error);
        message.error(t('errors.errorGetting', { model: t('templates.title') }));
      });

  const checkExistence = useDebounce(templateExists);

  return (
    <div className="templateName">
      <Form.Item
        hasFeedback
        help=""
        className="flex"
        name="name"
        label={t('credentialTemplateCreation.step1.templateName')}
        rules={[{ required: true }, { validator: checkExistence }]}
      >
        <Input placeholder={t('credentialTemplateCreation.step1.templateNamePlaceholder')} />
      </Form.Item>
    </div>
  );
};

TemplateName.propTypes = {
  api: PropTypes.shape({
    credentialTypesManager: credentialTypesManagerShape
  }).isRequired
};

export default withApi(TemplateName);
