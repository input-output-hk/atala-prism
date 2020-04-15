import React, { Component } from 'react';
import { AtalaPrismDemo } from 'atala-prism-demo';
import atalaLogo from '../../../../images/logo-atala-prism.svg';
import cardanoLogo from '../../../../images/logo-cardano.svg';
import './_style.scss';

class InteractiveMap extends Component {
  constructor(props) {
    super(props);
    this.setMapStep = this.setMapStep.bind(this);
  }

  componentDidMount() {
    window.addEventListener('click', () => {
      this.setMapStep();
    });
  }

  componentDidUpdate() {
    this.setMapStep();
  }

  setMapStep() {
    const { mapStep } = this.props;
    this.refs.demo.setStep(mapStep);
  }

  render() {
    return (
      <div className="MapContainer">
        <img src={atalaLogo} alt="Atala" className="AtalaLogo" />
        <AtalaPrismDemo ref="demo" />
        <img src={cardanoLogo} alt="Cardano" className="CardanoLogo" />
      </div>
    );
  }
}

export default InteractiveMap;
