# SleepingGiants

A dataset for dependencies susceptible to hiding Java deserialization gadgets and injection framework.

## Referencing this work

A preprint is available [here](https://arxiv.org/abs/2504.20485).

```bib
@inproceedings{kreyssig_sleepinggiants_2025,
  author = {Kreyssig, Bruno and Houy, Sabine and Riom, Timothée and Bartel, Alexandre},
  title = {Sleeping Giants - Activating Dormant Java Deserialization Gadget Chains through Stealthy Code Changes},
  doi = {10.1145/3719027.3765031},
  year = {2025},
  location = {Taipei, Taiwan},
  series = {CCS '25}
}
```

## Getting the Datasets

- **Section 3.3**: find the 4 gadget providing dependency datasets in [datasets/section3_final_datasets](datasets/section3_final_datasets)
- **Section 4.2**: modified dependencies are uploaded as release artifacts

## Usage

See [Evaluation.md](Evaluation.md) for reproducibility. 

We merged the two repositories for getting the serialization evolution and gadget injection framework. You can compile both artifacts in [3_serialization_evolution](3_serialization_evolution) and [4_gadget_inject](4_gadget_inject) with ``mvn clean package``.

Also notice that we moved the Tabby and AndroChain implementations used in the **scripts** to the [bin](bin) directory. This helps separate our artifacts from those in referenced works, i.e.:

```bib
@INPROCEEDINGS{10202660,
  author={Chen, Xingchen and Wang, Baizhu and Jin, Ze and Feng, Yun and Li, Xianglong and Feng, Xincheng and Liu, Qixu},
  booktitle={2023 53rd Annual IEEE/IFIP International Conference on Dependable Systems and Networks (DSN)}, 
  title={Tabby: Automated Gadget Chain Detection for Java Deserialization Vulnerabilities}, 
  year={2023},
  pages={179-192},
  doi={10.1109/DSN58367.2023.00028}
}
```

```bib
@inproceedings{10.1145/3611643.3616313,
author = {Srivastava, Prashast and Toffalini, Flavio and Vorobyov, Kostyantyn and Gauthier, Fran\c{c}ois and Bianchi, Antonio and Payer, Mathias},
title = {Crystallizer: A Hybrid Path Analysis Framework to Aid in Uncovering Deserialization Vulnerabilities},
year = {2023},
isbn = {9798400703270},
publisher = {Association for Computing Machinery},
address = {New York, NY, USA},
doi = {10.1145/3611643.3616313},
pages = {1586–1597},
numpages = {12},
keywords = {Deserialization vulnerabiltiies, Java, hybrid analysis},
location = {San Francisco, CA, USA},
series = {ESEC/FSE 2023}
}
```
(nothing in bin directory, since containerized)

```bib
@article{kreyssig2025deserialization,
  title={Deserialization Gadget Chains are not a Pathological Problem in Android: an In-Depth Study of Java Gadget Chains in AOSP},
  author={Kreyssig, Bruno and Riom, Timoth{\'e}e and Houy, Sabine and Bartel, Alexandre and McDaniel, Patrick},
  journal={arXiv preprint arXiv:2502.08447},
  year={2025}
}
```

When running the scripts copy the contents of the bin directory back to the scripts directory or adjust the paths in the scripts.


